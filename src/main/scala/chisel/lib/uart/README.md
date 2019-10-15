# Serial Port (UART)

This is a minimalistic version of a serial port, also called UART.

Testers are executed as follows and test the transmitter and
receiver:

```sbt "test:runMain chisel.lib.uart.TxTester"```

```sbt "test:runMain chisel.lib.uart.RxTester"```

Status: working in hardware (FPGA)
