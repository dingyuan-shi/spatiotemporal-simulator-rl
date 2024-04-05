SETUP_PATH="$(dirname $(readlink -f $0))"


# install algorithms in cpp
$SETUP_PATH/devtools/test_all.sh

# install local envs in java
$SETUP_PATH/envs/test_all.sh

