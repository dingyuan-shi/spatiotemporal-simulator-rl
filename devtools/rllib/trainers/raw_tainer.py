from rllib.agents import AbsAgent
from rllib.memories import AbsMemory


class RawTrainer:

    def __init__(self, env, agent: AbsAgent, mem: AbsMemory, episode, batch_size, learn_start=0, learn_freq=-1,
                 update_time=-1, render: bool = False, verbose: int = 1, refresh_freq: int = -1, max_epoch_iter=-1):
        self.env = env
        self.agent = agent
        self.mem = mem
        self.episode = episode
        self.batch_size = batch_size
        self.learn_start = learn_start
        self.learn_freq = learn_freq
        self.update_time = update_time
        self.render = render
        self.verbose = verbose
        self.refresh_freq = refresh_freq
        self.max_epoch_iter = max_epoch_iter
        if self.learn_freq == -1:
            self.learn_freq = self.batch_size

    def evaluation(self, env=None):
        self.agent.set_eval()
        if env is None:
            env = self.env
        total_reward: float = 0.
        for episode in range(100):
            # initialize
            episode_reward, step = 0., 0
            epoch_iter = 0
            observation, reward, done = env.reset(seed=0), 0., False
            while (not done) and (self.max_epoch_iter != -1 and epoch_iter < self.max_epoch_iter):
                if self.render:
                    env.render()
                epoch_iter += 1
                # take action here
                action = self.agent.choose_action(observation)
                next_observation, reward, done, truncated, info = env.step(action)
                done = done or truncated
                observation = next_observation
                step += 1
                episode_reward += reward
                if self.verbose >= 3:
                    print(f"step={step}, reward={reward}, info={info}")
            total_reward += episode_reward
            if self.verbose >= 2:
                print(f"ep={episode}, reward={episode_reward}")
        if self.verbose >= 1:
            print(f"total episodes:{100}, average reward per episode: {total_reward / 100}")
        if self.render:
            env.close()
        return total_reward / 100

    def train(self):
        self.agent.set_learn()
        total_step = 0
        for i in range(self.episode):
            state, done = self.env.reset(seed=0), False
            step, episode_reward, epoch_iter = 0, 0., 0
            while (not done) and (self.max_epoch_iter != -1 and epoch_iter < self.max_epoch_iter):
                epoch_iter += 1
                action, training_info = self.agent.choose_action(state)
                next_state, reward, done, truncated, info = self.env.step(action)
                done = done or truncated
                episode_reward += reward
                self.mem.push(state, action, reward, next_state, done, training_info)
                state = next_state
                step += 1
                total_step += 1
                if total_step >= self.learn_start and (step % self.learn_freq == 0 or done):
                    res = self.agent.update(*self.mem.sample(self.batch_size), update_time=self.update_time)
                    self.mem.end_update_behavior(res)
            if i % 50 == 0:
                print(f"reward after {i} episodes:{episode_reward}")
            self.mem.end_episode_behavior()
        self.mem.end_train_behavior()

    def train_gen(self):
        self.agent.set_learn()
        total_step = 0
        for i in range(self.episode):
            state, done = self.env.reset(seed=0), False
            step, episode_reward = 0, 0.
            while not done:
                action, training_info = self.agent.choose_action(state)
                next_state, reward, done, trunc, info = self.env.step(action)
                done = done or trunc
                episode_reward += reward
                self.mem.push(state, action, reward, next_state, done, training_info)
                state = next_state
                step += 1
                total_step += 1
                if self.refresh_freq != -1 and total_step % self.refresh_freq == 0:
                    params = yield self.agent.get_parameters()
                    self.agent.load_parameters(params)
                if total_step >= self.learn_start and (step % self.learn_freq == 0 or done):
                    res = self.agent.update(*self.mem.sample(self.batch_size), update_time=self.update_time)
                    self.mem.end_update_behavior(res)
            if i % 50 == 0:
                print(f"reward after {i} episodes: {episode_reward}")
            self.mem.end_episode_behavior()
        self.mem.end_train_behavior()
