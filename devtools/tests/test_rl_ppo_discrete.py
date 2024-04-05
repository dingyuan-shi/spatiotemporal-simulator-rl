import gym
import torch.cuda

from rllib.agents import PPODiscreteAgent
from rllib.memories import NaiveMemory
from .test_rl_common import seed_all, DiscretePolicy, Critic
from rllib.trainers import RawTrainer


def test_ppo_discrete():
    # device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device("cpu")
    print(device)
    env = gym.make("CartPole-v1", new_step_api=True)
    env.reset(seed=0)

    n_action = env.action_space.n
    state_dim = env.observation_space.shape[0]
    gamma = 0.9
    beta = 0.2
    eps = 0.1
    agent = PPODiscreteAgent(gamma, beta, eps, 0.01, state_dim, n_action, "ppo1", device,
                             DiscretePolicy(state_dim, n_action), Critic(state_dim), finite=True)
    trainer = RawTrainer(env, agent, NaiveMemory(), episode=500, batch_size=32, render=False, verbose=1, max_epoch_iter=200)
    trainer.train()
    trainer.evaluation()


if __name__ == "__main__":
    seed_all()
    test_ppo_discrete()
