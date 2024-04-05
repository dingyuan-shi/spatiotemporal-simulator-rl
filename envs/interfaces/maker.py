from .abs_env import AbsEnv


class Maker:
    
    @staticmethod
    def make(env_name: str, *args, **kwargs) -> AbsEnv:
        if env_name == "taxi":
            from .taxi_env import TaxiEnv
            return TaxiEnv(*args, **kwargs)
        elif env_name == "taxi-java":
            from .taxi_env_java import TaxiEnv
            return TaxiEnv(*args, **kwargs)
        elif env_name == "warehouse":
            from .warehouse_env import WarehouseEnv
            return WarehouseEnv(*args, **kwargs)
