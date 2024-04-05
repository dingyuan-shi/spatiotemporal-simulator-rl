from abc import ABC
from collections import namedtuple
from typing import List, Tuple, Union


class AbsMemory(ABC):
    Record = namedtuple("Record", ["state", "action", "reward", "next_state", "done", "training_info"])

    def __init__(self):
        pass

    def push(self, state, action, reward, next_state, done, training_info) -> None:
        raise NotImplementedError

    @staticmethod
    def get_attrs(attrs: Union[str, List[str]], recs: List[Record]) -> Union[List, Tuple]:
        if isinstance(attrs, str):
            return [getattr(each, attrs) for each in recs]
        return tuple([[getattr(each, attr) for each in recs] for attr in attrs])

    def sample(self, size: int) -> Tuple:
        raise NotImplementedError

    def end_update_behavior(self, *args, **kwargs):
        raise NotImplementedError

    def end_episode_behavior(self, *args, **kwargs):
        raise NotImplementedError

    def end_train_behavior(self, *args, **kwargs):
        raise NotImplementedError
