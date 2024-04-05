from .test_rl_common import seed_all, PendulumEnvWrapper, DiscretePolicy, ContinuousPolicy
from rllib.agents import ReinforceDiscreteAgent, ReinforceContinuousAgent
import torch
import gym
from rllib.memories import NaiveMemory
from rllib.trainers import RawTrainer


def test_reinforce_discrete():
    # device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device("cpu")
    print(device)
    env = gym.make("CartPole-v1", new_step_api=True)
    env.reset(seed=0)
    n_action = env.action_space.n
    state_dim = env.observation_space.shape[0]
    gamma = 0.9
    agent = ReinforceDiscreteAgent(gamma, state_dim, n_action, 0.01, device, DiscretePolicy(state_dim, n_action))
    trainer = RawTrainer(env, agent, NaiveMemory(), 200, batch_size=999, render=False, verbose=1, max_epoch_iter=200)
    trainer.train()
    trainer.evaluation()


def test_reinforce_continuous():
    # device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device("cpu")
    print(device)
    env = PendulumEnvWrapper(gym.make("Pendulum-v1", new_step_api=True))
    env.reset(seed=0)
    n_action = env.action_space.shape[0]
    state_dim = env.observation_space.shape[0]
    action_range = (env.action_space.low, env.action_space.high)
    gamma = 0.9
    agent = ReinforceContinuousAgent(gamma, state_dim, n_action, 0.002, device, action_range,
                                     ContinuousPolicy(state_dim, n_action, action_range))
    trainer = RawTrainer(env, agent, NaiveMemory(), episode=200, learn_freq=9999, batch_size=999, render=False,
                         verbose=1, max_epoch_iter=200)
    trainer.train()
    trainer.evaluation(env.unwrapper())


if __name__ == "__main__":
    seed_all()
    test_reinforce_discrete()
    test_reinforce_continuous()
