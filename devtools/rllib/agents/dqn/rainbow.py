import copy
from typing import List, Any

import torch
import torch.nn as nn
import numpy as np
from torch import Tensor
from .abstract_agent import AbsDQNAgent


class RainbowAgent(AbsDQNAgent):

    def __init__(self, net: nn.Module, device, state_dim, n_action, lr, eps, gamma, v_min, v_max,
                 n_atom, multi_step, update_freq, reset_noise_freq, finite: bool = True) -> None:
        super().__init__(eps, finite)
        self.UPDATE_FREQ: int = update_freq
        self.RESET_NOISE_FREQ: int = reset_noise_freq

        self.device = device
        self.state_dim = state_dim
        self.gamma = gamma
        self.update_cnt: int = 0
        self.learn_cnt: int = 0

        self.q_learned = net.to(self.device)
        self.q_target = copy.deepcopy(net).to(self.device)
        self._update_target()

        self.n_action = n_action
        self.optimizer = torch.optim.Adam(self.q_learned.parameters(), lr=lr)

        self.v_min = v_min
        self.v_max = v_max
        self.n_atom = n_atom
        if self.n_atom > 1:
            self.delta = (self.v_max - self.v_min) / (self.n_atom - 1)
        self.quantile = torch.linspace(self.v_min, self.v_max, self.n_atom).to(self.device)
        self.device = device
        self.n = multi_step

    def choose_action(self, single_observ: np.array) -> Any:
        single_observ = Tensor(single_observ).view(-1).to(self.device)
        self.learn_cnt += 1
        if self.learn_cnt % self.RESET_NOISE_FREQ == 0:
            self.learn_cnt = 0
            self.q_learned.reset_noise()

        if self.learn and np.random.uniform(0, 1) < self.eps:
            action = np.random.randint(0, self.n_action)
        else:
            with torch.no_grad():
                # 1维 长度是n_action 表示每个action期望的Q
                if self.n_atom == 1:
                    action = self.q_learned(single_observ.view(-1, self.state_dim)).argmax().item()
                else:
                    action = (self.q_learned(single_observ.view(-1, self.state_dim)) * self.quantile).sum(2).argmax(
                        dim=1).item()
        self.update_eps()
        if self.learn:
            return action, None
        else:
            return action

    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = -1) -> None:
        batch_size = len(states)
        states, rewards, next_states, dones = \
            self.to_tensor(states, rewards, next_states, dones, self.device, self.state_dim, self.finite)
        actions = Tensor(actions).to(self.device, dtype=torch.long).view(-1, 1)
        indices, memory, _ = infos
        self.q_target.reset_noise()
        if self.n_atom == 1:
            q_next_actions = self.q_learned(next_states).detach().argmax(dim=1, keepdim=True)
            q_targets = rewards + (1. - dones) * self.gamma * self.q_target(
                next_states).gather(dim=1, index=q_next_actions)
            td_errors = q_targets - self.q_learned(states).gather(dim=1, index=actions)
            loss = torch.square(td_errors)
        else:
            probs = self.q_target(next_states).detach()
            q_next_actions = (self.q_learned(next_states).detach() * self.quantile).sum(dim=2).argmax(dim=1)
            q_s_a_probs = probs[np.arange(batch_size), q_next_actions]
            new_quantiles = rewards + (1. - dones) * np.power(self.gamma, self.n) * self.quantile.view(-1, self.n_atom)
            new_quantiles.clamp_(self.v_min, self.v_max)
            new_quantiles = (new_quantiles - self.v_min) / self.delta
            quantile_l = new_quantiles.floor().to(torch.int64)
            quantile_u = new_quantiles.ceil().to(torch.int64)
            quantile_l[(quantile_l > 0) * (quantile_l == quantile_u)] -= 1  # 对于整除且不是左端点情况 -1
            quantile_u[(quantile_u < (self.n_atom - 1)) * (quantile_l == quantile_u)] += 1  # 对于整除且不是右端点的情况 + 1
            offset = torch.linspace(0, (batch_size - 1) * self.n_atom, batch_size).view(
                -1, 1).expand(batch_size, self.n_atom).to(actions)
            m = torch.zeros(batch_size, self.n_atom, dtype=torch.double, device=self.device)
            m.view(-1).index_add_(0, (offset + quantile_l).view(-1),
                                  (q_s_a_probs * (quantile_u.double() - new_quantiles)).view(-1))
            m.view(-1).index_add_(0, (offset + quantile_u).view(-1),
                                  (q_s_a_probs * (new_quantiles - quantile_l.double())).view(-1))
            td_errors = torch.sum(-m * ((self.q_learned(states).log())[
                np.arange(batch_size), actions.view(-1)]), dim=1)
            loss = td_errors

        weights = memory.get_weights(indices)
        if weights is not None:
            loss = (torch.Tensor(weights).to(self.device) * loss).mean()
        else:
            loss = loss.mean()

        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()
        self._update_target()
        return td_errors.detach().cpu().numpy()

    def _update_target(self):
        if self.update_cnt % self.UPDATE_FREQ == 0:
            self.q_target.load_state_dict(self.q_learned.state_dict())
            self.update_cnt = 0
        self.update_cnt += 1

    def get_parameters(self) -> Any:
        return self.q_learned.state_dict()

    def load_parameters(self, params: Any) -> None:
        self.q_learned.load_state_dict(params)
        self.update_target_network_hard(self.q_target, self.q_learned, 1, 1)
