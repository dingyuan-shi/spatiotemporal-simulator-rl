from typing import List, Tuple

from .abstract_memory import AbsMemory


class NaiveMemory(AbsMemory):

    def __init__(self):
        super().__init__()
        self.records: List[AbsMemory.Record] = []

    def push(self, state, action, reward, next_state, done, training_info) -> None:
        self.records.append(AbsMemory.Record(state, action, reward, next_state, done, training_info))

    def sample(self, size: int) -> Tuple:
        size = min(size, len(self.records))
        recs = self.records[:size]
        return self.get_attrs(["state", "action", "reward", "next_state", "done", "training_info"], recs)

    def end_update_behavior(self, *args, **kwargs):
        self.records.clear()

    def end_episode_behavior(self, *args, **kwargs):
        pass

    def end_train_behavior(self, *args, **kwargs):
        pass
