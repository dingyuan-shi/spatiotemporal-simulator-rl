from typing import Tuple, Any
import torch
from rllib.agents import NAFAgent
from rllib.memories import NaiveReplayMemory
from rllib.models import AbsNafQ
from rllib.trainers import RawTrainer
from .test_rl_common import PendulumEnvWrapper, seed_all
import gym
import torch.nn as nn
import torch.nn.functional as func


class NafQ(AbsNafQ):

    def __init__(self, state_dim: int, action_dim: int, action_range: Tuple) -> None:
        super().__init__(state_dim, action_dim, action_range)
        self.feature_dim = 128
        self.share_layer1 = nn.Linear(self.state_dim, self.feature_dim)
        self.v_layer = nn.Linear(self.feature_dim, 1)
        self.mu_layer = nn.Linear(self.feature_dim, self.action_dim)
        # l can derive a sigma = l * l^T
        self.l_layer = nn.Linear(self.feature_dim, self.action_dim * self.action_dim)

    def forward(self, state: torch.Tensor) -> Tuple[Any, Any, Any]:
        feature = func.relu(self.share_layer1(state))
        mu = torch.tanh(self.mu_layer(feature)) * self.action_width + self.action_mid
        v = self.v_layer(feature).view(-1, 1)
        l_raw = self.l_layer(feature).view(-1, self.action_dim, self.action_dim)
        return mu, l_raw, v


def test_naf():
    # device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device("cpu")
    print(device)
    env = PendulumEnvWrapper(gym.make("Pendulum-v1", new_step_api=True))
    env.reset(seed=0)
    state_dim = env.observation_space.shape[0]
    action_dim = env.action_space.shape[0]
    action_range = (env.action_space.low, env.action_space.high)
    gamma = 0.9

    agent = NAFAgent(gamma, NafQ(state_dim, action_dim, action_range), device, action_range, state_dim, action_dim,
                     lr=0.001, eps=(1, 0.01), tau=0.01, finite=False)
    trainer = RawTrainer(env, agent, NaiveReplayMemory(buffer_size=5000), episode=200, learn_freq=1, learn_start=5000,
                         batch_size=32, update_time=-1, render=False, verbose=1, max_epoch_iter=200)
    trainer.train()
    trainer.evaluation()


if __name__ == "__main__":
    seed_all()
    test_naf()
