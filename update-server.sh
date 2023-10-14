#!/bin/bash
set -x
cd ~/Code/Scala/portfolzio
ssh root@portfolzio "echo 'Stopping service...'; systemctl stop portfolzio; echo 'Service stopped.'"
sbt assembly
scp target/scala-3.3.0/portfolzio-assembly-1.0.0.jar portfolzio@portfolzio:~/portfolzio.jar
ssh root@portfolzio "echo 'Starting service...'; systemctl start portfolzio; echo 'Service started!'"
