Hamming Error-Correcting Code (ECC)
===================================

This package contains some primitives which are used to generate two modules,
EccGenerate and EccCorrect.  These implement the generation and detection/
correction sides of a Hamming error correcting code.  This code is a
single-bit-correcting code.  With the optional parity bit it can also detect
but not correct double bit errors.

See https://en.wikipedia.org/wiki/Hamming_code for more information about
Hamming codes.

## Components

- EccGenerate

The EccGenerate module takes two parameters, a type parameter which indicates 
Chisel data type to generate an ECC on, and doubleBit, which when true 
generates an additional parity bit used for double-bit error detection.

In addition to the optional parity output, the EccGenerate has an "eccOut"
output which contains the error check bits for the input data.

- EccCheck

The EccCheck module takes the same two parameters as EccGenerate above.

As inputs, EccCheck takes the original dataIn, as well as the parity bits
in eccIn and the optional parity bit in parIn.  From dataIn and eccIn
the check block computes an "error syndrome", which is the bit location
(starting from 1) of the incorrect bit among the received data.  An
error syndrome of zero indicates no error.

The eccCheck block also creates a corrected dataOut signal, which contains
the original dataIn after any single-bit errors have been removed.  If
there is more than one bit error dataOut will not be correct.

If only checking is desired without error correction, leave the dataOut
signal unconnected and use the original uncorrected dataIn bits.  The
error correction logic will be removed during FIRRTL optimization.