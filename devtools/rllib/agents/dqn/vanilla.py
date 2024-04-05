import copy
from typing import List, Any, Tuple
from torch import Tensor
import torch
import torch.nn as nn
import numpy as np

from .abstract_agent import AbsDQNAgent


class VanillaDQNAgent(AbsDQNAgent):

    def __init__(self, gamma: float, state_dim: int, n_action: int, device: torch.device, q_net: nn.Module,
                 eps: Tuple[float, float], lr: float, target_update_freq: int, finite: bool = True):
        super().__init__(eps, finite)
        self.state_dim = state_dim
        self.n_action = n_action
        self.device = device
        self.q_learn = q_net
        self.q_target = copy.deepcopy(q_net)

        self.q_learn = self.q_learn.to(self.device)
        self.q_target = self.q_target.to(self.device)

        self.gamma = gamma
        self.step = 0
        self.update_counter = 0
        self.target_update_freq = target_update_freq
        self.optimizer = torch.optim.Adam(self.q_learn.parameters(), lr=lr)

    def choose_action(self, single_state: np.array) -> Any:
        single_state = Tensor(single_state).to(self.device).view(-1)
        with torch.no_grad():
            if self.learn:
                if np.random.uniform(0, 1) < self.eps:
                    action = np.random.randint(self.n_action)
                else:
                    action = self.q_learn(single_state).view(-1).argmax().item()
                # decay epsilon
                self.update_eps()
                return action, None
            action = self.q_learn(single_state).view(-1).argmax().item()
        return action

    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = -1) -> Any:
        # calculate td errors
        states, rewards, next_states, dones = self.to_tensor(states, rewards, next_states, dones,
                                                             self.device, self.state_dim, self.finite)
        actions = Tensor(np.array(actions)).to(self.device, torch.long).view(-1, 1)
        with torch.no_grad():
            q_next = self.q_target(next_states).view(-1, self.n_action).max(1, keepdim=False)[0]
        q_eval = self.q_learn(states).view(-1, self.n_action).gather(dim=1, index=actions).view(-1)
        td_errors = torch.square(q_eval - (rewards.view(-1) + self.gamma * (1. - dones.view(-1)) * q_next)).mean()
        self.optimizer.zero_grad()
        td_errors.backward()
        self.optimizer.step()

        self.update_counter += 1
        if self.update_target_network_hard(self.q_target, self.q_learn, self.update_counter, self.target_update_freq):
            self.update_counter = 0

    def get_parameters(self) -> Any:
        return self.q_learn.state_dict()

    def load_parameters(self, params: Any) -> None:
        self.q_learn.load_state_dict(params)
        self.update_target_network_hard(self.q_target, self.q_learn, 1, 1)
