import torch.nn as nn
import torch
import torch.nn.functional as func
import numpy as np


class Linear(nn.Module):

    def __init__(self, n_input: int, n_output: int, bias: bool = True, device: torch.device = None) -> None:
        super().__init__()
        self._n_input = n_input
        self._n_output = n_output
        self._weight = nn.Parameter(torch.empty(n_output, n_input))
        self._bias = nn.Parameter(torch.empty(n_output)) if bias else None
        self._reset_parameters()
        self.device = device

    def _reset_parameters(self):
        # use fan-in fan out init style
        bound: float = 1. / np.sqrt(self._n_input)
        nn.init.uniform_(self._weight, -bound, bound)
        if self._bias is not None:
            nn.init.uniform_(self._bias, -bound, bound)

    def forward(self, observation: torch.Tensor) -> torch.Tensor:
        return func.linear(input=observation, weight=self._weight, bias=self._bias)


class NoisyLinear(Linear):
    @staticmethod
    def rescale(v: torch.Tensor):
        return v.sign() * v.abs().sqrt()

    def __init__(self, n_input: int, n_output: int, bias: bool = True, device: torch.device = None,
                 init_std: float = 0.1) -> None:
        super().__init__(n_input, n_output, bias, device)
        self._init_std = init_std
        self._weight_scale = nn.Parameter(torch.empty(n_output, n_input))
        self._weight_noise = torch.Tensor(torch.empty(n_output, n_input))
        self._bias_scale = nn.Parameter(torch.empty(n_output)) if bias else None
        self._bias_noise = torch.Tensor(torch.empty(n_output)) if bias else None
        self._reset_parameters()
        self._reset_noise_parameters()
        self._reset_noise()

    def _reset_noise_parameters(self):
        nn.init.constant_(self._weight_scale, val=self._init_std / np.sqrt(self._n_input))
        if self._bias_scale is not None:
            nn.init.constant_(self._bias_scale, val=self._init_std / np.sqrt(self._n_output))

    def reset_noise(self):
        base_input = NoisyLinear.rescale(torch.randn(self._n_input)).to(self.device)
        base_output = NoisyLinear.rescale(torch.randn(self._n_output)).to(self.device)
        self._weight_noise = base_output.outer(base_input)
        if self._bias is not None:
            self._bias_noise = base_output

    def forward(self, observation: torch.Tensor) -> torch.Tensor:
        if self._bias is not None:
            # print(self._weight_noise.device) # cpu
            return func.linear(observation, self._weight + self._weight_scale * self._weight_noise,
                               self._bias + self._bias_scale * self._bias_noise)
        else:
            return func.linear(observation, self._weight + self._weight_scale * self._weight_noise, None)
