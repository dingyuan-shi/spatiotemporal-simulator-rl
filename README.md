# Data-driven Spatiotemporal Simulator for Reinforcement Learning Method (DSS)
Souce code for VLDB 2024 Demo Paper "Data-driven Spatiotemporal Simulator for Reinforcement Learning Methods"
## requirement
gcc/g++ version 7.5.0 (Ubuntu 7.5.0-6ubuntu2)   
java version "1.8.0_381"  
javac 1.8.0_381  
conda 23.7.2  
Apache Maven 3.6.3  

Other jave dependencies can be found at pom.xml under their corresponding directories.

Python dependencies can be found at requirement list.

## quick start
```bash
## view taxi
cd /path/to/taxi_vision
export DSS_DIR='/path/to/DSS' && export FLASK_APP=server && flask run

## view warehouse
cd /path/to/warehouse_vision
export DSS_DIR=/path/to/DSS/ && python server.py

## launch warehouse simulation natively
export DSS_DIR='/path/to/data' && java -jar warehouse-1.0-SNAPSHOT.jar GMAP synA
```
The whole structure is organized as below

```bash
- devtools
|-- comblib
|-- rllib
|-- tests
- envs
|-- interfaces
|-- engines
|-- resources
|-- tests
- visualize
|-- taxi_visiom
|-- warehouse_vision
```

## Devtools
This submodule has three main parts: comblib, rllib, tests

### `comblib`
This submodule contains classic combinatorial algorithm about `matching` and `planning`.
`matching` has matching strategies such as greedy, Hungary, and Kunh-Munkres algorithm.
`planning` has planning strategies such as bredth first search (BFS), depth first search (DFS) and Dijkstra's algorithm.

All algorithms are implemented in C++ with a python interface for easy use.

For a independent use, simply conduct the `install.sh` under the directory comblib and you can import it as a module.

### `rllib`
This submodule contains reinforcement learning algorithms such as 
- Q-learning based methods: vanilla DQN, DDPG, Rainbow, NafQ
- Policy gradient based methods: REINFORCE, PPO (both continous and discrete)

This submodule is implemented in pure python and can be directly import.

### `tests`
This is testing module for checking the correctness and functionality.
To conduct a whole test for the devtools, please run
```bash
cd /path/to/devtools/
bash test_all.sh
```

## Envs
### ``interfaces``
This submodule contains a maker, you can use this to make an enviroment.

We build two enviroment: "taxi" and "warehouse", you can also build your own environment.

### ``resources``
Our warehouse enviroment are implemented in java, so we need to compile it to a jar and put it into engines.

### ``engines``
Java jars and python pacakge for environments.

### `tests`
Test files for two environments.


## visualize
### ``taxi_vision``
It provide validation information board for taxi order dispatching.

### ``warehouse_vision``
It provide validation information board for warehouse task scheduling.