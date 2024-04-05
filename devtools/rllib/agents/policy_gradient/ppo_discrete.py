import torch
import torch.nn as nn
import numpy as np
from typing import List, Union, Any
from .abstract_agents import PPOAgent
from rllib.models import AbsDiscretePolicy


class PPODiscreteAgent(PPOAgent):

    def __init__(self, gamma: float, beta: float, eps: float, lr: float, state_dim: int, n_action: int, strategy: str,
                 device: torch.device, policy_model: AbsDiscretePolicy, critic_model: nn.Module, finite: bool = True) -> None:
        super().__init__(gamma, beta, eps, lr, state_dim, n_action, strategy, device,
                         policy_model, critic_model, finite)

    def choose_action(self, single_state: np.array) -> Any:
        single_state = torch.Tensor(single_state).to(self.device).view(-1, self.state_dim)
        with torch.no_grad():
            if self._learn:
                sample_prob_dist = self.sample(single_state).squeeze()
                info = [self.critic(single_state).squeeze().item(), sample_prob_dist.cpu().numpy()]
                return torch.distributions.Categorical(sample_prob_dist).sample().item(), info
            else:
                return torch.distributions.Categorical(self.policy(single_state)).sample().squeeze().item()

    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = -1) -> Any:
        batch_size = len(states)
        old_state_values = np.array([each[0] for each in infos])
        sample_prob_dist = np.array([each[1] for each in infos])

        dones = [done and self.finite for done in dones]
        discount_rewards = np.zeros(batch_size)
        process_value = self.critic(torch.Tensor(next_states[-1]).to(self.device)).detach().cpu().item()
        discount_rewards[-1] = rewards[-1] + (1 - dones[-1]) * self.gamma * process_value
        for k in reversed(range(batch_size - 1)):
            discount_rewards[k] = discount_rewards[k + 1] * self.gamma + rewards[k]
        rewards = discount_rewards

        for i in range(0, batch_size, 200):
            left, right = i, min(i + batch_size, batch_size)
            batch_rewards = torch.Tensor(np.array(rewards[left:right])).to(self.device).view(-1)
            batch_states = torch.Tensor(np.array(states[left: right])).to(self.device)
            batch_actions = torch.Tensor(np.array(actions[left: right])).view(-1, 1).to(self.device, dtype=torch.long)

            # calculate advantage
            batch_old_state_values = torch.Tensor(old_state_values[left: right]).to(self.device).view(-1)
            advantages = batch_rewards - batch_old_state_values

            # calculate critic loss
            critic_loss = (self.critic(batch_states) - batch_rewards).square().mean()

            # calculate actor loss

            # calculate importance ratio
            probs_policy_single = self.policy.log_probs(batch_states, batch_actions).exp().view(-1)
            probs_policy = self.policy(batch_states)

            batch_sample_prob_dist = torch.Tensor(sample_prob_dist[left: right]).to(self.device)
            probs_sample = batch_sample_prob_dist.gather(dim=1, index=batch_actions).view(-1)

            important_ratio = probs_policy_single / probs_sample
            raw_actor_targets = (important_ratio * advantages)

            if self.strategy == "ppo1":
                actor_target = raw_actor_targets.mean() - \
                               self.beta * torch.kl_div(probs_policy.log(), batch_sample_prob_dist).sum(1).mean()
            else:
                # ppo2 continuous force to use ppo2
                clamp_ratio = torch.clamp(important_ratio, 1 - self.eps, 1 + self.eps)
                actor_target = torch.min(raw_actor_targets, clamp_ratio * advantages).mean()
            loss: torch.Tensor = -actor_target + critic_loss

            self.critic_optimizer.zero_grad()
            self.policy_optimizer.zero_grad()
            loss.backward()
            self.policy_optimizer.step()
            self.critic_optimizer.step()

        # finish update reset sampler
        self.reset_sampler()
