import numpy as np
from typing import Optional
from .abstract_memory import AbsMemory


class SumTree:

    def __init__(self, capacity_pow: int) -> None:
        capacity = np.power(2, capacity_pow) - 1
        self.priorities = np.zeros(capacity,)
        self.offset = capacity // 2

    def insert(self, idx: int, pri: float) -> None:
        delta = pri - self.priorities[idx + self.offset]
        idx += self.offset
        # propagate up
        while idx >= 0:
            self.priorities[idx] += delta
            idx = (idx - 1) // 2

    def sample(self, batch_size: int) -> np.array:
        indices = np.zeros(batch_size, dtype=np.int64)
        segment_len = self.priorities[0] / batch_size
        for i in range(batch_size):
            r = np.random.uniform(i * segment_len, (i + 1) * segment_len)
            idx = 0
            while idx < self.offset:
                left = idx * 2 + 1
                if r <= self.priorities[left]:
                    idx = left
                else:
                    r -= self.priorities[left]
                    idx = idx * 2 + 2
            indices[i] = idx - self.offset
        return indices


class PERMemory(AbsMemory):

    def __init__(self, alpha: float, multi_step: int, gamma: float, state_dim: int, capacity: int) -> None:
        super().__init__()
        self.state_dim = state_dim
        self.gamma = gamma
        self.p = 0
        self.alpha = alpha
        assert capacity > 2
        size = 2
        powers = 1
        while size < capacity:
            size *= 2
            powers += 1
        self.SIZE = size
        self.states = np.zeros([size, self.state_dim])
        self.actions = np.zeros([size, 1], dtype=np.int64)
        self.rewards = np.zeros([size, 1])
        self.next_states = np.zeros([size, self.state_dim])
        self.dones = np.zeros([size, 1], dtype=np.int64)
        self.training_info = [None for _ in range(size)]

        self.tree = SumTree(powers + 1)
        self.full = False
        self.n = multi_step
        self.prio_m = 0.1

        self.last_sample_indices = None

    def push(self, state, action, reward, next_state, done, training_info) -> None:
        self.p = (self.p + 1) % self.SIZE
        self.full = self.full or self.p == 0
        self.states[self.p][:] = state.reshape(-1, )
        self.actions[self.p] = action
        self.rewards[self.p] = reward
        self.next_states[self.p] = next_state.reshape(-1, )
        self.dones[self.p] = int(done)
        self.training_info[self.p] = training_info
        self.tree.insert(self.p, np.power((np.abs(self.prio_m) + 0.01), self.alpha))

    def sample(self, batch_size: int):
        indices = self.tree.sample(batch_size)
        self.last_sample_indices = indices
        rewards_to_go = self.rewards[indices].reshape(-1)
        discount = np.ones(batch_size)
        dones_to_go = self.dones.reshape(-1)[indices]
        end_indices = indices.copy()
        for k in range(1, self.n - 1):
            end_indices += (1 - dones_to_go)
            discount *= self.gamma
            rewards_to_go += dones_to_go * discount * self.rewards.view(-1)[indices + k]
            dones_to_go *= (1 - self.dones.view(-1)[indices + k])
        dones_to_go = dones_to_go.tolist()
        training_infos = [self.training_info[i] for i in indices]
        return self.states[indices].reshape(-1, self.state_dim), \
               self.actions[indices], rewards_to_go.reshape(-1, 1), \
               self.next_states[end_indices].reshape(-1, self.state_dim), \
               dones_to_go, (indices, self, training_infos)

    def get_weights(self, indices: np.array) -> Optional[np.array]:
        return self.tree.priorities[indices + self.tree.offset]

    def end_update_behavior(self, td_errors):
        for i, index in enumerate(self.last_sample_indices):
            self.tree.insert(index, np.power(np.abs(td_errors[i]) + 0.01, self.alpha))

    def end_episode_behavior(self):
        pass

    def end_train_behavior(self):
        pass
