#!/bin/bash

mvn3 clean install
java -jar target/benchmarks.jar -wi 10 -i 10 -f 3
