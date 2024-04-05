import torch.nn as nn
import torch
import numpy as np
from typing import Any, Tuple
from .abstract_agents import ReinforceAgent
from rllib.models import AbsContinuousPolicy


class ReinforceContinuousAgent(ReinforceAgent):

    def __init__(self, gamma: float, observ_dim: int, n_action: int, lr: float, device: torch.device,
                 action_range: Tuple[Any, Any], model: AbsContinuousPolicy) -> None:
        super().__init__(gamma, observ_dim, n_action, lr, device, model)
        self.action_range = torch.Tensor(action_range[0]).to(self.device), torch.Tensor(action_range[1]).to(self.device)

    def choose_action(self, single_observ: np.array) -> Any:
        single_observ = torch.Tensor(single_observ).to(self.device)
        with torch.no_grad():
            mu, sigma = self.net(single_observ)
            action = torch.distributions.Normal(mu, sigma).sample()
            action = torch.clamp(action, self.action_range[0], self.action_range[1])
            action = action.cpu().numpy().reshape(self.n_action)
            if self.learn:
                return action, None
            else:
                return action
