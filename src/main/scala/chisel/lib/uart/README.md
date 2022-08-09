# Serial Port (UART)

This is a minimalistic version of a serial port, also called UART.

Testers are executed as follows and test the transmitter and
receiver:

```sbt "testOnly chisel.lib.uart.UartTxTests"```

```sbt "testOnly chisel.lib.uart.UartRxTests"```

Status: working in hardware (FPGA)
