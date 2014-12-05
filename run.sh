#!/bin/bash

mvn3 clean install
java -jar target/benchmarks.jar -wi 5 -i 5 -f 1
