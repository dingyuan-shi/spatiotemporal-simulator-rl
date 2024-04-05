import random
from typing import List, Tuple
from .abstract_memory import AbsMemory


class NaiveReplayMemory(AbsMemory):

    def __init__(self, buffer_size: int):
        super().__init__()
        self.records: List[AbsMemory.Record] = []
        self.buffer_size = buffer_size
        self.pointer = 0

    @property
    def full(self):
        return self.buffer_size == len(self.records)

    def push(self, state, action, reward, next_state, done, training_info) -> None:
        record = AbsMemory.Record(state, action, reward, next_state, done, training_info)
        if len(self.records) < self.buffer_size:
            self.records.append(record)
            if len(self.records) == self.buffer_size:
                self.pointer = 0
        else:
            self.records[self.pointer] = record
            self.pointer = (self.pointer + 1) % self.buffer_size

    def sample(self, size: int) -> Tuple:
        # experience replay
        size = min(size, len(self.records))
        return self.get_attrs(["state", "action", "reward", "next_state", "done", "training_info"],
                              random.sample(self.records, size))

    def end_update_behavior(self, *args, **kwargs):
        pass

    def end_episode_behavior(self, *args, **kwargs):
        pass

    def end_train_behavior(self, *args, **kwargs):
        pass
