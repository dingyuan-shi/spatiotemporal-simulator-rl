import gym
from typing import Any, Tuple
import torch
import numpy as np
import random
from rllib.models import AbsDiscretePolicy, AbsContinuousPolicy, AbsVCritic
import torch.nn.functional as func
import torch.nn as nn


def seed_all():
    random.seed(0)
    np.random.seed(0)
    torch.manual_seed(0)
    torch.cuda.manual_seed(0)
    torch.cuda.manual_seed_all(0)


class PendulumEnvWrapper:
    def __init__(self, obj: gym.Env):
        self.__obj = obj

    def unwrapper(self):
        return self.__obj

    def reset(self, seed=0):
        return self.__obj.reset(seed=seed)

    def seed(self, seed: int):
        return self.__obj.reset(seed=seed)

    def step(self, action):
        next_state, reward, done, trunc, info = self.__obj.step(action)
        return next_state, (reward + 8.) / 8., done, trunc, info  # switch for unify with cartpole

    def __getattr__(self, item):
        return getattr(self.__obj, item)


class DiscretePolicy(AbsDiscretePolicy):

    def __init__(self, state_dim: int, n_action: int) -> None:
        super().__init__(state_dim, n_action)
        n_hidden: int = 20
        self.fc1 = nn.Linear(state_dim, n_hidden)
        self.fc2 = nn.Linear(n_hidden, n_action)

    def forward(self, states: torch.Tensor) -> torch.Tensor:
        states = states.view(-1, self.state_dim)
        hidden = func.relu(self.fc1(states))
        return func.softmax(self.fc2(hidden), dim=-1)

    def log_probs(self, batch_states: torch.Tensor, batch_actions: torch.Tensor) -> torch.Tensor:
        return self(batch_states).gather(dim=1, index=batch_actions).log()


class ContinuousPolicy(AbsContinuousPolicy):

    def __init__(self, state_dim: int, action_dim: int, action_range: Tuple[Any, Any]) -> None:
        super().__init__(state_dim, action_dim, action_range)
        hidden_size = 128
        self.shared_fc = nn.Linear(state_dim, hidden_size)
        self.mu_fc = nn.Linear(hidden_size, action_dim)
        self.sigma_fc = nn.Linear(hidden_size, action_dim)

    def forward(self, states: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        states = states.view(-1, self.state_dim)
        hidden_layer = func.relu(self.shared_fc(states))
        mu = torch.tanh(self.mu_fc(hidden_layer)) * self.action_width + self.action_mid
        sigma = func.softplus(self.sigma_fc(hidden_layer))  # ensure positive
        return mu, sigma

    def log_probs(self, batch_states: torch.Tensor, batch_actions: torch.Tensor) -> torch.Tensor:
        mus, sigmas = self.forward(batch_states)
        return torch.distributions.Normal(mus, sigmas).log_prob(batch_actions)


class Critic(AbsVCritic):
    def __init__(self, state_dim: int) -> None:
        super().__init__(state_dim)
        hidden = 128
        self.fc1 = nn.Linear(state_dim, hidden)
        self.fc2 = nn.Linear(hidden, 1)
        self.state_dim = state_dim

    def forward(self, states: torch.Tensor) -> torch.Tensor:
        states = states.view(-1, self.state_dim)
        hidden = func.relu(self.fc1(states))
        out = self.fc2(hidden)
        return out


class NaiveQNet(nn.Module):
    def __init__(self, state_dim: int, n_action: int):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(state_dim, 128),
            nn.ReLU(),
            nn.Linear(128, n_action),
        )

    def forward(self, states):
        return self.net(states)