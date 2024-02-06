# IIR Filter

Simple fixed point IIR filter with pipelined computation.

Tests are run as follow:
```sbt "testOnly chisel.lib.iirfilter.SimpleIIRFilterTest -- -DwriteVcd=1"```
```sbt "testOnly chisel.lib.iirfilter.RandomSignalTest -- -DwriteVcd=1"```
