# FIR Filter

Simple fixed point FIR filter with pipelined computation.

Tests are run as follow:
```sbt "testOnly chisel.lib.firfilter.SimpleFIRFilterTest -- -DwriteVcd=1"```
```sbt "testOnly chisel.lib.firfilter.RandomSignalTest -- -DwriteVcd=1"```
