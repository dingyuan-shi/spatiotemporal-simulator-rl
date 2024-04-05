import copy
from typing import List, Any, Tuple
import numpy as np
import torch
import torch.nn as nn
from torch import Tensor

from .abstract_agent import AbsDQNAgent


class NAFAgent(AbsDQNAgent):

    def __init__(self, gamma: float, net: nn.Module, device: torch.device, action_range: Tuple,
                 state_dim: int, action_dim: int, lr: float, eps: tuple, tau: float, finite: bool = True):
        super().__init__(eps, finite)
        self.device = device
        self.gamma = gamma
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.q_learn = net
        self.q_target = copy.deepcopy(net)

        self.q_learn = self.q_learn.to(self.device)
        self.q_target = self.q_target.to(self.device)

        self.noise_base = torch.ones(self.action_dim).to(self.device)

        self.action_range = Tensor(action_range[0]).to(self.device), Tensor(action_range[1]).to(self.device)

        self.tau = tau

        self.optimizer = torch.optim.Adam(self.q_learn.parameters(), lr=lr)

    def choose_action(self, single_state: np.array) -> Any:
        single_state = Tensor(single_state).to(self.device).view(-1)
        with torch.no_grad():
            if self.learn:
                mu, _, _ = self.q_learn(single_state)
                action = torch.distributions.Normal(mu, scale=self.noise_base * self.eps).sample()
                action = torch.clamp(action.view(-1), self.action_range[0], self.action_range[1])
                self.update_eps()
                return action.cpu().numpy(), None
            else:
                mu, _, _ = self.q_learn(single_state)
                action = torch.clamp(mu.view(-1), self.action_range[0], self.action_range[1])
            return action.cpu().numpy()

    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = -1) -> Any:
        states, rewards, next_states, dones = self.to_tensor(states, rewards, next_states, dones,
                                                             self.device, self.state_dim, self.finite)
        actions = Tensor(np.array(actions)).to(self.device).view(-1, self.action_dim)

        with torch.no_grad():
            _, _, vs = self.q_target(next_states)
            q_next = rewards.view(-1) + (1. - dones) * self.gamma * vs.view(-1)

        mus, l_raws, vs = self.q_learn(states)
        l_features = torch.tril(l_raws, diagonal=-1) + l_raws.exp() * torch.eye(self.action_dim, device=self.device)
        sigmas = torch.bmm(l_features, l_features.transpose(1, 2))
        mu_as = (actions - mus).view(-1, 1, self.action_dim)
        q_eval = -0.5 * torch.bmm(torch.bmm(mu_as, sigmas), mu_as.transpose(1, 2)).view(-1) + vs.view(-1)

        loss = (q_next - q_eval).square().mean()
        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        self.update_target_network_soft(self.q_target, self.q_learn, self.tau)

    def get_parameters(self) -> Any:
        return self.q_learn.state_dict()

    def load_parameters(self, params: Any) -> None:
        self.q_learn.load_state_dict(params)
        self.update_target_network_hard(self.q_target, self.q_learn, 1, 1)
