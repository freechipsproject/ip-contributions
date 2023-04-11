# Serial Peripheral Interface (SPI)

Minimalistic version of SPI interface.

Features:
- Modes 00, 01, 10 and 11
- Word length in parameters

Status: Tested in mode 00, 01, 10, 11 with cocotb

# Test

prerequisites:
- cocotb
- cocotbext-spi

* Generate verilog sources
```sbt "runMain chisel.lib.spi.MasterOneCS"```

* Run testbench
```
cd src/test/scala/chisel/lib/spi
make
```

* Display wave
```gtkwave dump.vcd```
