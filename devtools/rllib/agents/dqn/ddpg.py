import copy
from typing import Any, List, Tuple

import torch
import torch.nn as nn
from torch import Tensor
import numpy as np

from .abstract_agent import AbsDQNAgent


class DDPGAgent(AbsDQNAgent):

    def __init__(self, gamma: float, state_dim: int, action_dim: int, device: torch.device, eps: Tuple, tau: float,
                 lr: float, action_range: Tuple[Any, Any], actor: nn.Module, critic: nn.Module, finite=True):
        super().__init__(eps, finite)
        self.gamma = gamma
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.actor = actor
        self.actor_target = copy.deepcopy(actor)
        self.critic = critic
        self.critic_target = copy.deepcopy(critic)
        self.device = device

        self.noise_base = torch.ones(self.action_dim).to(self.device)

        self.actor = self.actor.to(self.device)
        self.actor_target = self.actor_target.to(self.device)
        self.critic = self.critic.to(self.device)
        self.critic_target = self.critic_target.to(self.device)

        self.action_range = Tensor(action_range[0]).to(self.device), Tensor(action_range[1]).to(self.device)

        self.tau = tau
        self.critic_optimizer = torch.optim.Adam(self.critic.parameters(), lr=lr)
        self.actor_optimizer = torch.optim.Adam(self.actor.parameters(), lr=lr)

    def choose_action(self, single_state: np.array) -> np.array:
        single_state = Tensor(single_state).to(self.device).view(-1)
        with torch.no_grad():
            out = self.actor(single_state).view(-1)
            if self.learn:
                out = torch.distributions.Normal(out, self.noise_base * self.eps).sample()
                out = torch.clamp(out.view(-1), self.action_range[0], self.action_range[1])
                self.update_eps()
                return out.cpu().numpy(), None
            else:
                out = torch.clamp(out, self.action_range[0], self.action_range[1])
                return out.cpu().numpy()

    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = -1) -> Any:
        states, rewards, next_states, dones = AbsDQNAgent.to_tensor(states, rewards, next_states, dones,
                                                                    self.device, self.state_dim, self.finite)
        actions = Tensor(np.array(actions)).to(self.device).view(-1, self.action_dim)
        # calculate TD errors
        with torch.no_grad():
            next_actions = self.actor_target(next_states).view(-1, self.action_dim)
            state_next_action_next = torch.concat([next_states, next_actions], dim=1)
            target_value = rewards + (1. - dones) * self.gamma * self.critic_target(state_next_action_next)
        td_errors = (target_value - self.critic(torch.concat([states, actions], dim=1))).square().mean()
        self.critic_optimizer.zero_grad()
        td_errors.backward()
        self.critic_optimizer.step()

        # train actor
        actor_loss = -self.critic(torch.concat([states, self.actor(states)], dim=1)).mean()
        self.actor_optimizer.zero_grad()
        actor_loss.backward()
        self.actor_optimizer.step()

        # update target
        self.update_target_network_soft(self.critic_target, self.critic, self.tau)
        self.update_target_network_soft(self.actor_target, self.actor, self.tau)

    def get_parameters(self) -> Any:
        return self.actor.state_dict(), self.critic.state_dict()

    def load_parameters(self, params: Any) -> None:
        self.actor.load_state_dict(params[0])
        self.critic.load_state_dict(params[1])
        self.update_target_network_hard(self.actor_target, self.actor, 1, 1)
        self.update_target_network_hard(self.critic_target, self.critic, 1, 1)
