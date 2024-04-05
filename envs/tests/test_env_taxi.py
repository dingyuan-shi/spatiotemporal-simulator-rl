from interfaces.maker import Maker
import sys
sys.path.append("../devtools/")
from comblib.matching import greedy

# a reward greedy Agent
class Agent:
    def __init__(self) -> None:
        # initialize here
        pass

    def action(self, mo):
        driver_id_no, order_id_no = dict(), dict()
        driver_no_id, order_no_id = dict(), dict()
        order_cnt, driver_cnt = 0, 0
        edges = []
        for od in mo:
            driver_id, order_id = od.driver_id, od.order_id
            if driver_id not in driver_id_no:
                driver_id_no[driver_id] = driver_cnt
                driver_no_id[driver_cnt] = driver_id
                driver_cnt += 1
            if order_id not in order_id_no:
                order_id_no[order_id] = order_cnt
                order_no_id[order_cnt] = order_id
                order_cnt += 1
            edges.append(((driver_id_no[driver_id], order_id_no[order_id]), od.order_driver_distance))
        matching_action = []
        match_res = greedy(edges, is_max=False)
        for driver_no in match_res:
            order_no = match_res[driver_no]
            matching_action.append((order_no_id[order_no], driver_no_id[driver_no]))
        return matching_action



if __name__ == "__main__":
    # initialize an env
    env = Maker.make("taxi")
    agent = Agent()
    matching_action = None
    repo_action = None
    observation, done = env.reset(date=20161106, start_time=14400, finish_time=20000), False
    while not done:
        dispatch_observ = observation.get('dispatch_observ', None)
        matching_action = agent.action(dispatch_observ) if dispatch_observ else None
        observation, reward, done, info = env.step(dict(matching=matching_action, repo=repo_action))
