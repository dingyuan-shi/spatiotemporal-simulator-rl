import collections
from multiprocessing import connection, Process
from .raw_tainer import RawTrainer


class SubTrainer(Process):

    def __init__(self, trainer: RawTrainer, pipe_end: connection.Connection):
        super().__init__()
        self.agent = trainer.agent
        self.trainer = trainer
        self.pipe_end = pipe_end
        self.last_params = None

    def run(self):
        # initialize by global parameters
        _, params = self.receive()
        self.agent.load_parameters(params)
        self.last_params = params
        gen = self.trainer.train_gen()
        params = None
        while True:
            try:
                # training and waiting for global update
                params = gen.send(params)
            except StopIteration:
                break
            # send parameter changes
            delta_params = collections.OrderedDict()
            for k in params:
                delta_params[k] = params[k] - self.last_params[k]
            self.last_params = params
            self.send("data", delta_params)
            _, params = self.receive()
        self.send("end", None)

    def send(self, cmd, data):
        self.pipe_end.send((cmd, data))

    def receive(self):
        return self.pipe_end.recv()
