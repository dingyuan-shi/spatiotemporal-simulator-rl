import abc

from typing import Any, Tuple
from abc import ABC, abstractmethod


class AbsEnv(ABC):
    
    @abstractmethod
    def reset(self, *args, **kwargs) -> Any:
        raise NotImplementedError

    @abstractmethod
    def step(self, action: Any) -> Tuple[Any, Any, bool, Any]:
        raise NotImplementedError

    @abstractmethod
    def capture(self, *args, **kwargs):
        raise NotImplementedError


class AbsJarEnv(AbsEnv, ABC):

    def __init__(self, env_class: str, classpath: str="") -> None:
        import jnius_config
        super().__init__()
        jnius_config.add_classpath(classpath)
        # must import after jnius_config, otherwise the JVM has already running and cannot change configurations.
        from jnius import autoclass
        self.rawEnv = autoclass(env_class)
