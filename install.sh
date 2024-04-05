SETUP_PATH="$(dirname $(readlink -f $0))"

# install algorithms in cpp
$SETUP_PATH/devtools/comblib/install.sh

# install local envs in java
$SETUP_PATH/envs/install_all.sh
