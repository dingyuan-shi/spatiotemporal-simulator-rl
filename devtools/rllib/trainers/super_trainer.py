import copy
from multiprocessing import Pipe
from typing import List
from .subtrainer import RawTrainer, SubTrainer
import time


class SuperTrainer:

    def __init__(self, trainers: List[RawTrainer]):
        self.n_trainer = len(trainers)
        assert self.n_trainer > 1
        pipes = [Pipe(duplex=True) for _ in range(self.n_trainer)]
        self.pipes = [each[0] for each in pipes]
        self.subtrainers = [SubTrainer(trainer, pipes[i][1]) for i, trainer in enumerate(trainers)]
        self.global_param = copy.deepcopy(trainers[0].agent.get_parameters())

    def execute(self, interval=1):
        # start all
        going_set = set(range(self.n_trainer))
        for trainer in self.subtrainers:
            trainer.start()
        while len(going_set) > 0:
            # send all
            for idx in going_set:
                self.pipes[idx].send(("global_param", self.global_param))
            time.sleep(interval)
            delete_idx = set()
            round_receive = 0
            for idx in going_set:
                avail = self.pipes[idx].poll()  # async
                if not avail:
                    continue
                cmd, data = self.pipes[idx].recv()
                if cmd == "end":
                    delete_idx.add(idx)
                else:
                    round_receive += 1
                    for key in self.global_param:
                        self.global_param[key] = (1 - 0.01) * self.global_param[key] + 0.01 * data[key]
            print(f"receive from {round_receive} agents")
            going_set.difference_update(delete_idx)
