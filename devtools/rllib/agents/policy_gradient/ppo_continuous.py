import torch
import torch.nn as nn
import numpy as np
from typing import List, Any, Tuple
from torch import Tensor
from .abstract_agents import PPOAgent
from rllib.models import AbsContinuousPolicy


class PPOContinuousAgent(PPOAgent):

    def __init__(self, gamma: float, eps: float, lr: float, state_dim: int, n_action: int, device: torch.device,
                 policy_model: AbsContinuousPolicy, critic_model: nn.Module, action_range: Tuple, finite: bool = True) -> None:
        super().__init__(gamma, 0., eps, lr, state_dim, n_action, "ppo2", device,
                         policy_model, critic_model, finite)

        self.action_range = torch.Tensor(action_range[0]).to(self.device), torch.Tensor(action_range[1]).to(self.device)

    def choose_action(self, single_state: np.array) -> Any:
        single_state = torch.Tensor(single_state).to(self.device).view(-1, self.state_dim)
        with torch.no_grad():
            mus, sigma = self.sample(single_state)
            distr = torch.distributions.Normal(mus.squeeze(), sigma.squeeze())
            action = distr.sample()
            if self._learn:
                action = torch.clamp(action, self.action_range[0], self.action_range[1])
                info = [self.critic(single_state).squeeze().item(),
                        distr.log_prob(action).exp().cpu().item()]
                return action.cpu().numpy(), info
            else:
                return torch.clamp(action, self.action_range[0], self.action_range[1]).cpu().numpy()

    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = 10) -> Any:
        states, rewards, next_states, dones = \
            self.to_tensor(states, rewards, next_states, dones, self.device, self.state_dim, self.finite)
        rewards = rewards.view(-1)

        batch_size = rewards.size()[0]
        discount_rewards = np.zeros(batch_size)
        process_value = self.critic(torch.Tensor(next_states[-1]).to(self.device)).detach().cpu().item()
        discount_rewards[-1] = rewards[-1] + (1. - dones[-1]) * self.gamma * process_value
        for k in reversed(range(batch_size - 1)):
            discount_rewards[k] = discount_rewards[k + 1] * (1. - dones[-1]) * self.gamma + rewards[k]
        rewards = Tensor(discount_rewards).view(-1).to(self.device)

        actions = torch.Tensor(np.array(actions)).view(-1, self.n_action).to(self.device)
        old_state_values = torch.Tensor(np.array([each[0] for each in infos])).to(self.device)
        sample_prob_distr = torch.Tensor(np.array([each[1] for each in infos])).to(self.device)
        # calculate advantage
        advantages = rewards - old_state_values
        for _ in range(10):
            # calculate actor loss
            critic_loss = (self.critic(states).view(-1) - rewards).square().mean()
            self.critic_optimizer.zero_grad()
            critic_loss.backward()
            self.critic_optimizer.step()
            # calculate importance ratio
            probs_policy_single = self.policy.log_probs(states, actions).exp().view(-1)
            probs_sample = sample_prob_distr.view(-1)
            important_ratio = probs_policy_single / probs_sample
            clamp_ratio = torch.clamp(important_ratio, 1 - self.eps, 1 + self.eps)
            actor_target = torch.min(important_ratio * advantages, clamp_ratio * advantages).mean()
            actor_loss = -actor_target

            self.policy_optimizer.zero_grad()
            actor_loss.backward()
            self.policy_optimizer.step()

        # finish update reset sampler
        self.reset_sampler()
