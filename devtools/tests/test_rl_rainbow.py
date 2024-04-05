
import torch
import torch.nn as nn
import gym
from rllib.agents import RainbowAgent
from .test_rl_common import seed_all
from rllib.models import NoisyLinear, Linear
from rllib.memories import PERMemory
import torch.nn.functional as func
from rllib.trainers import RawTrainer


class DQN(nn.Module):

    def __init__(self, state_dim: int, n_action: int, n_atom: int, reset_noise_freq: int) -> None:
        super().__init__()
        self.state_dim = state_dim
        self.n_atom = n_atom
        self.n_action = n_action
        self.noise = reset_noise_freq >= 0

        # # create hidden layers for v and a
        fc_single = NoisyLinear if self.noise else Linear
        self.v_fc = fc_single(state_dim, 50, True)
        self.a_fc = fc_single(state_dim, 30, True)

        # create out layers for v and a
        self.v_fc_out = fc_single(50, n_atom, bias=True)
        self.a_fc_out = fc_single(30, n_action * n_atom, bias=True)

    def reset_noise(self):
        if not self.noise:
            return
        self.v_fc.reset_noise()
        self.a_fc.reset_noise()
        self.v_fc_out.reset_noise()
        self.a_fc_out.reset_noise()

    def forward(self, vector: torch.Tensor) -> torch.Tensor:
        vector = vector.view(-1, self.state_dim)  # 保证输入二维 行是batch size 列是observation的长度

        v_vector = func.relu(self.v_fc(vector))
        v_out = self.v_fc_out(v_vector)  # v_out是batch size行 1列

        a_vector = func.relu(self.a_fc(vector))
        a_out = self.a_fc_out(a_vector)  # v_out是batch size行 1列

        if self.n_atom > 1:
            v_out = v_out.view(-1, 1, self.n_atom)
            a_out = a_out.view(-1, self.n_action, self.n_atom)

        # dueling DQN
        q = v_out + a_out - a_out.mean(dim=1, keepdim=True)
        # dim=1表示把列方向的一串数字求平均，避免求平均之后的向量被压维度 batch size行 1列
        # a_out - a_out.mean() 非常智能 他自动把每一行求和 而由于列数不一样 他自动进行element-wise求和
        return q if self.n_atom == 1 else func.softmax(q, dim=2)


def test_rainbow():
    env = gym.make("CartPole-v1", new_step_api=True)
    env.reset(seed=0)

    # device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    device = torch.device("cpu")
    print(device)
    state_dim = env.observation_space.shape[0]
    n_action = env.action_space.n
    # 调参技巧 对于gamma 像cartpole这种可以设为0.95 因为每一步都很重要
    # 对于epsilon 一般都是decay的 前期的大小可以大一点鼓励探索 但问题不难的情况下好像也不用太大
    # 小 lr + 大episode可以让训练更稳定
    # start learn一般设置成batch一样吧 不然一开始训练的样本太少了
    lr = 0.001
    epsilon = (0.5, 0.05)
    gamma = 0.95
    v_min = -10
    v_max = 10
    n_atom = 10  # 1 degrades to non=distributional DQN must >= 1 and integer
    alpha = 0.6  # -1 degrades to non-PER sampling, default 0.6
    multi_step = 2  # 1 degrades to one step DQN
    update_freq = 20
    reset_noise_freq = -1  # -1 degrades to non-noisy linear

    agent = RainbowAgent(DQN(state_dim, n_action, n_atom, reset_noise_freq), device, state_dim, n_action, 0.005, epsilon,
                         gamma, v_min, v_max, n_atom, multi_step, update_freq, reset_noise_freq)
    memory = PERMemory(alpha, multi_step, gamma, state_dim, capacity=4000)
    trainer = RawTrainer(env, agent, memory, episode=500, batch_size=50, learn_start=50, learn_freq=5, update_time=-1,
                         render=False, verbose=1, max_epoch_iter=200)
    trainer.train()
    trainer.evaluation()


if __name__ == "__main__":
    seed_all()
    test_rainbow()
