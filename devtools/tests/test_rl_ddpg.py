import torch
import torch.nn as nn
import torch.nn.functional as func
from torch import Tensor
import gym
from rllib.agents import DDPGAgent
from rllib.models import AbsDeterministicContinuousActor
from rllib.trainers import RawTrainer
from rllib.memories import NaiveReplayMemory
from .test_rl_common import seed_all, PendulumEnvWrapper, Critic


class ContinuousActor(AbsDeterministicContinuousActor):

    def __init__(self, state_dim: int, action_dim: int, action_range) -> None:
        super().__init__(state_dim, action_dim, action_range)
        hidden = 128
        self.fc1 = nn.Linear(self.state_dim, hidden)
        self.fc2 = nn.Linear(hidden, self.action_dim)

    def forward(self, states: Tensor) -> Tensor:
        states = states.view(-1, self.state_dim)
        hidden_layer = func.relu(self.fc1(states))
        out = self.fc2(hidden_layer)
        out = torch.tanh(out) * self.action_width + self.action_mid
        return out


def test_ddpg():
    # device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device("cpu")
    env = PendulumEnvWrapper(gym.make("Pendulum-v1", new_step_api=True))
    env.reset(seed=0)
    state_dim = env.observation_space.shape[0]
    action_dim = env.action_space.shape[0]
    tau = 0.01
    gamma = 0.9
    lr = 0.001
    action_range = env.action_space.low, env.action_space.high
    agent = DDPGAgent(gamma, state_dim, action_dim, device, (1, 0.01), tau, lr, action_range,
                      ContinuousActor(state_dim, action_dim, action_range), Critic(state_dim + action_dim),
                      finite=False)
    trainer = RawTrainer(env, agent, NaiveReplayMemory(buffer_size=5000), episode=200, learn_freq=1, learn_start=5000,
                         batch_size=32, update_time=-1, render=False, verbose=1, max_epoch_iter=200)
    trainer.train()
    trainer.evaluation(env)


if __name__ == "__main__":
    seed_all()
    test_ddpg()
