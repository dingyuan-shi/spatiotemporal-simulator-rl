# build comblib
SETUP_PATH="$(dirname $(readlink -f $0))"
bash ${SETUP_PATH}/comblib/install.sh

# testing
echo "begin testing..."

echo "test comblib algorithms"
python -m tests.test_comblib
echo "test completed"

echo "test rl algorithms"

echo "test vanilla DQN..."
python -m tests.test_rl_vanilla_dqn

echo "test DDPG..."
python -m tests.test_rl_ddpg

echo "test rainbow..."
python -m tests.test_rl_rainbow

echo "test NafQ..."
python -m tests.test_rl_naf

echo "test REINFORCE..."
python -m tests.test_rl_reinforce

echo "test PPO continuous..."
python -m tests.test_rl_ppo_continuous

echo "test PPO discrete..."
python -m tests.test_rl_ppo_discrete
