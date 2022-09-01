#!/bin/bash
run_1c=true
run_1python=true
run_21c=true
run_21python=true
run_22=true
run_3=true

if [ "$run_1c" = true ] ; then
    echo "================== 1 (C) ==================" >> output
    for i in {600..3000..400}
        do
            echo "1. $i x $i"
            echo -e "1-$i" >> output
            echo -e "1\n$i" | ./test >> output
        done
fi

if [ "$run_1python" = true ] ; then
    echo "================== 1 (Python) ==================" >> output
    for i in {600..3000..400}
        do
            echo "1. $i x $i"
            echo -e "1-$i" >> output
            echo -e "1\n$i" | python3 matrixproduct.py >> output
        done
fi

if [ "$run_21c" = true ] ; then
    echo "================== 2.1 (C) ==================" >> output
    for i in {600..3000..400}
        do
            echo "2.1. $i x $i"
            echo -e "2-$i" >> output
            echo -e "2\n$i" | ./test >> output
        done
fi


if [ "$run_21python" = true ] ; then
    echo "================== 2.1 (Python) ==================" >> output
    for i in {600..3000..400}
        do
            echo "2.1. $i x $i"
            echo -e "2-$i" >> output
            echo -e "2\n$i" | python3 matrixproduct.py >> output
        done
fi

if [ "$run_22" = true ] ; then
    echo "================== 2.2 ==================" >> output
    for i in {4096..10240..2048}
        do
            echo "2.2. $i x $i"
            echo -e "2-$i" >> output
            echo -e "2\n$i" | ./test >> output
        done
fi


if [ "$run_3" = true ] ; then
    echo "================== 3 ==================" >> output
    for i in {4096..10240..2048}
        do
            for j in 128 256 512
            do
                echo "3. $i x $i (block size: $j)"
                echo -e "3-$i-$j" >> output
                echo -e "3\n$i\n$j" | ./test >> output
            done
        done
fi