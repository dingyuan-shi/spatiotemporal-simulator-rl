echo "please make sure you have install devtools!"

SETUP_PATH="$(dirname $(readlink -f $0))"
ENGINE_SRC_CODE_PATH="$SETUP_PATH/resources"
ENGINE_CONFIG_PATH="$SETUP_PATH/configs"
ENGINES_PATH="$SETUP_PATH/engines"
for i in `ls $ENGINE_SRC_CODE_PATH`
do
    if [[ $i =~ "deprecated" ]] 
    then
        echo "deprecated $i"
    else
        ENGINE_PROJECT_DIR="$ENGINE_SRC_CODE_PATH/$i"
        mvn clean package -f "$ENGINE_PROJECT_DIR/pom.xml"
        cp "$ENGINE_PROJECT_DIR/target/$i-1.0-SNAPSHOT.jar" $ENGINES_PATH
    fi
done