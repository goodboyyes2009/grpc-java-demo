#!/bin/sh
#定义环境变量及程序执行参数
JAVA_HOME="/usr/local/java/jdk1.8.0_211"

if [ $# -ne 1 ] ; then
    echo "USAGE: ./stop MainClass"
    exit 1;
fi

#程序执行主类赋值
#APP_MAINCLASS="netty.in.action.chapter1.PlainNioEchoServer"
APP_MAINCLASS=$@

#初始化psid变量（全局）
psid=0
checkpid() {
   echo $APP_MAINCLASS
   javaps=`$JAVA_HOME/bin/jps -l | grep $APP_MAINCLASS`

   if [ -n "$javaps" ]; then
      psid=`echo $javaps | awk '{print $1}'`
   else
      psid=0
   fi
}

#停止程序
stop(){
    checkpid
    if [ $psid -ne 0 ]; then
	echo "Stopping $psid $APP_MAINCLASS ........................"
	kill -9 $psid
	if [ $? -eq 0 ]; then
	    echo "$APP_MAINCLASS [Stop Success]"
	else
	    echo "$APP_MAINCLASS [Stop Failure]"
	fi
    else
	echo "Warn: $APP_MAINCLASS is not running"
    fi
}

stop