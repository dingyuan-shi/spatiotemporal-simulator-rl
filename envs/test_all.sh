SETUP_PATH="$(dirname $(readlink -f $0))"

# build engines
bash $SETUP_PATH/install_all.sh

python -m tests.test_env_taxi
python -m tests.test_env_warehouse