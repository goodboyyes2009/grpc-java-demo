#!/bin/sh
#-------------------------------------------------------------------------------------------------------------
#该脚本的使用方式为-->[sh start.sh]
#该脚本可在服务器上的任意目录下执行,不会影响到日志的输出位置等
#-------------------------------------------------------------------------------------------------------------
JAVA_HOME="/usr/local/java/jdk1.8.0_211"
if [ $# -lt 1 ] ; then
  echo "USAGE: ./start.sh mainClass 参数"
  echo " e.g.: ./start.sh com.xxx.xxx.xxx 参数"
  exit 1;
fi
#APP_HOME=/data/apps/tactics
SHELL_FOLDER=$(dirname $(readlink -f "$0"))
APP_HOME=$(dirname $SHELL_FOLDER)
APP_LOG=${APP_HOME}/logs

# 如果log目录不存在,则创建
if [ ! -d "$APP_LOG" ]; then
  mkdir "$APP_LOG"
fi

CLASSPATH=$APP_HOME/conf
for jarFile in ${APP_HOME}/lib/*.jar;
do
   CLASSPATH=$CLASSPATH:$jarFile
done

#参数处理
APP_MAIN=$1
shift
params=$@
JAVA_OPTS="-Duser.timezone=GMT+8 -server -Xms2048m -Xmx2048m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+HeapDumpOnOutOfMemoryError -Xloggc:${APP_LOG}/gc.log"


startup(){
  aparams=($params)
  #echo "params len "${#aparams[@]}
  len=${#aparams[@]}
  for ((i=0;i<$len;i++));do
    echo "第${i}参数:${aparams[$i]}"; 
    str=$str" "${aparams[$i]};
  done
  echo "Starting $APP_MAIN"
  echo "$JAVA_HOME/bin/java $JAVA_OPTS -XX:OnOutOfMemoryError='chmod 644 *.hprof'  -classpath $CLASSPATH $APP_MAIN $str"
  $JAVA_HOME/bin/java $JAVA_OPTS -XX:OnOutOfMemoryError='chmod 644 *.hprof' -classpath $CLASSPATH $APP_MAIN $str
}
startup
