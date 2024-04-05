import torch
import gym

from rllib.trainers import SuperTrainer, RawTrainer
from .test_rl_common import seed_all, NaiveQNet
from rllib.agents import VanillaDQNAgent
from rllib.memories import NaiveReplayMemory


def test_process():
    # device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device("cpu")
    print(device)
    dummy_env = gym.make("CartPole-v0")
    state_dim = dummy_env.observation_space.shape[0]
    n_action = dummy_env.action_space.n
    del dummy_env
    gamma = 0.9
    eps = (0.5, 0.01)
    lr = 0.001
    target_update_freq = 5
    finite = True
    buffer_size = 4000
    episode = 500
    batch_size = 32
    learn_start = 4000
    learn_freq = 10
    update_time = -1
    render = False
    verbose = 1
    refresh_freq = 10
    trainers = [RawTrainer(
                            gym.make("CartPole-v0"),
                            VanillaDQNAgent(gamma, state_dim, n_action, device, NaiveQNet(state_dim, n_action),
                                            eps, lr, target_update_freq, finite),
                            NaiveReplayMemory(buffer_size),
                            episode, batch_size, learn_start, learn_freq, update_time, render, verbose, refresh_freq
                            ) for _ in range(3)]
    st = SuperTrainer(trainers)
    st.execute(interval=2)


if __name__ == "__main__":
    seed_all()
    test_process()
