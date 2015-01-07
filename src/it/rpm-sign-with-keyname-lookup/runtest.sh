for N in `seq -w 0 999`;
do
    echo -n Starting test: ${N} at $(date)
    echo ""
    mvn  -s settings.xml  verify -DdisableSigning=false -Dgpg.homedir=../../test-classes/gnupg -Ptestkey > build.log
    if [ $? -ne 0 ]; then
      exit 1
    fi

    grep "Pass phrase is good" build.log
    if [ $? -ne 0 ]; then
      exit 1
    fi
done
