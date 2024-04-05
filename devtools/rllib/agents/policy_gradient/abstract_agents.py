import copy
from abc import ABC
from rllib.agents import AbsAgent
import numpy as np
from typing import Any, List
import torch
import torch.nn as nn


class PPOAgent(AbsAgent, ABC):

    def __init__(self, gamma: float, beta: float, eps: float, lr: float, state_dim: int, n_action: int, strategy: str,
                 device: torch.device, policy_model: nn.Module, critic_model: nn.Module, finite: bool = True) -> None:
        super().__init__(finite)
        self.state_dim = state_dim
        self.n_action = n_action
        self.gamma = gamma
        self.beta = beta
        self.eps = eps
        self.device = device
        self.strategy = strategy  # ppo1 or ppo2

        self.policy = policy_model
        self.sample = copy.deepcopy(policy_model).to(device)
        self.policy = self.policy.to(device)
        self.critic = critic_model.to(device)
        self.reset_sampler()
        self.policy_optimizer = torch.optim.Adam(self.policy.parameters(), lr=lr)
        self.critic_optimizer = torch.optim.Adam(self.critic.parameters(), lr=lr)

    def reset_sampler(self) -> None:
        self.sample.load_state_dict(self.policy.state_dict())

    def get_parameters(self) -> Any:
        return self.policy.state_dict(), self.critic.state_dict()

    def load_parameters(self, params: Any) -> None:
        self.policy.load_state_dict(params[0])
        self.critic.load_state_dict(params[1])


class ReinforceAgent(AbsAgent, ABC):

    def __init__(self, gamma: float, state_dim: int, n_action: int, lr: float, device: torch.device, model: nn.Module,
                 finite: bool = True):
        super().__init__(finite)
        self.state_dim = state_dim
        self.device = device
        self.net = model.to(self.device)
        self.gamma = gamma
        self.n_action = n_action
        self.optimizer = torch.optim.Adam(self.net.parameters(), lr=lr)

    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = -1) -> Any:
        episode_len = len(states)

        discount_rewards = np.zeros(episode_len)
        discount_rewards[-1] = rewards[-1]
        for k in reversed(range(episode_len - 1)):
            discount_rewards[k] = discount_rewards[k + 1] * self.gamma + rewards[k]
        discount_rewards = (discount_rewards - discount_rewards.mean()) / (discount_rewards.std() + 0.01)
        rewards = discount_rewards
        
        # accumulate loss in a batch based manner to too-large matrix
        size = 200
        loss = torch.zeros(1).to(self.device)
        for i in range(0, episode_len, size):
            left, right = i, min(episode_len, i + size)
            batch_rewards = torch.Tensor(np.array(rewards[left: right])).to(self.device).view(-1, 1)
            batch_states = torch.Tensor(np.array(states[left: right])).to(self.device).view(-1, self.state_dim)
            if self.__class__.__name__ == "ReinforceDiscreteAgent":
                batch_actions = torch.tensor(np.array(actions[left: right])).to(self.device, torch.long).view(-1, 1)
            else:
                batch_actions = torch.tensor(np.array(actions[left: right])).to(self.device).view(-1, self.n_action)
            log_probs = self.net.log_probs(batch_states, batch_actions)
            loss += (batch_rewards * log_probs).sum()

        loss = torch.sum(loss)
        loss = -loss / episode_len
        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

    def get_parameters(self) -> Any:
        return self.net.state_dict()

    def load_parameters(self, params: Any) -> None:
        self.net.load_state_dict(params)
