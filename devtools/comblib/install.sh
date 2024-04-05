SETUP_PATH="$(dirname $(readlink -f $0))"
SETUP_FILE="$SETUP_PATH/setup.py"
for sub_module in "matching" "planning"
do
    SUB_MODULE_SETUP_PATH="$SETUP_PATH/$sub_module"
    python $SETUP_FILE build_ext --build-lib "$SUB_MODULE_SETUP_PATH" --build-temp "$SUB_MODULE_SETUP_PATH/build/" $sub_module
    rm -rf "$SUB_MODULE_SETUP_PATH/build/"
    rm "$SUB_MODULE_SETUP_PATH/cpplib/$sub_module.cpp"
done