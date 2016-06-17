HAWK
====

Hybrid Question Answering (hawk) -- is going to drive forth the OKBQA vision of hybrid question answering using Linked Data and full-text indizes. 

Performance benchmarks can be done on the QALD-5 hybrid benchmark (test+train)


Restful Service
===
``curl localhost:8080/search?q=What+is+the+capital+of+Germany+%3F``
will return a UUID.


``curl http://localhost:8080/status?UUID=00000000-0000-0000-0000-000000000001`` gives you status updates

Building and Run HAWK
===
```
mvn clean package -DskipTests
java -jar target/hawk-0.1.0.jar

```
Or, execute the script below to build and run:
```
./build-run-HAWK.sh
```

Test HAWK 
====
Execute the script to Test against HAWK REST Service:
```
./test-HAWK.sh
```

More Reference
===
Supplementary material concerning the evaluation and implementation of HAWK can be found here
http://aksw.org/Projects/HAWK.html
