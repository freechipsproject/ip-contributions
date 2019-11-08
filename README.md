Chisel IP Contributions
=======================

If you are here you are hopefully considering contributing some useful chisel modules to
the community. This is the place to do it.

## Current Contributions

| Package | Type | Developer | Descpription |
| --- | --- | --- | --- |
| BitonicSorter   | internal |  Steve Burns  | A combinational sort generator for arbitrarily typed Vectors |
| IterativeCordic   | internal |  Harrison Liew  | An iterative Cordic implementation |
| aes_chisel   | maven | Sergiu Mosanu | Implementation of the Advanced Encryption Standard (AES)<br> [How to get it](#aes_chisel)  |
| uart | internal | Martin Schoeberl | A basic serial port (UART) |
| fifo | internal | Martin Schoeberl | Variations of FIFO queues |
| spi2wb | maven | Fabien Marteau | Drive a wishbone master bus with SPI |

### Getting Started

To begin with you should have some working Chisel code. 
Adding it is designed to be as simple as possible.
There are two main ways to go about this.
1. Creating a new package here in the src/main/scala and src/test/scala and place your circuit and it's unit tests in those respective directories. [More about this](more-on-locally-provided-contributions)
1. Create an external repository and distribute it via the [Maven](https://maven.apache.org/), let us review it and provides links to it.

### External Contributions

---

#### aes_chisel
- **Developer** Sergiu Mosanu
- **Repository** [https://github.com/hplp/aes_chisel](https://github.com/hplp/aes_chisel)
- **Versions** 3.2, 3.1
- **Sbt Dependency** com.github.hplp" %% "aes_chisel" % "3.2.0"

**aes_chisel**'s goals are to:
* Implement an open-source, transparent, secure encryption module in Chisel
* Equip developers with ready-to-use, efficient, parameterizable, encryption macros for RISC-V systems
* Compare performance and resource utilization of generated HDL with pure Verilog and Vivado HLS
* Compare code size with Verilog, Python, C++ and Vivado HLS as a index of development productivity

#### spi2wb
- **Developer** Fabien Marteau
- **Repository** [https://github.com/Martoni/spi2wb](https://github.com/Martoni/spi2wb)
- **Versions** 1.3, 1.2, 1.1, 1.0
- **Sbt Dependency** com.github.hplp" %% "spi2wb" % "1.3"

**spi2wb**'s goals are to:
* Drive a wishbone master bus with SPI
* Be easy to integrate in little FPGA
* Be used with simple computer and USB tools like BusPirate
* Avoid usage of softcore under the FPGA to drive wishbone peripherals

---

## More on Locally Provided Contributions
### The Requirements

- Code Style should reasonably follow the default style guide used by the Chisel3 project. See [the chisel3 scalastyle-config](https://github.com/freechipsproject/chisel3/blob/master/scalastyle-config.xml)
- Unit Tests should be provided that reasonable confidence that a contribution works
- Useful README.md should be added in your package directory with contribution specific info.
- Licensing this code is still a  **WIP**

### Vetting

### Publishing

## License
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
