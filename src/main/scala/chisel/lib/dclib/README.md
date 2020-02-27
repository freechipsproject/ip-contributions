DecoupledIO Library
===================

This is a library of components which are useful to building systems with DecoupledIO interfaces.

## Timing Closure Model

These components are written to support either a registered-output
or a registered-input-and-output timing methodology.  

A registered-output timing methodology requires that at a block boundary,
all outputs of the block are required to come directly from a flip-flop.
Block inputs are allowed to be combinatorial, usually with some duty-cycle
reservation.

A registered-input-and-output requires that all inputs to a block
go directly to a flip-flop, and all outputs come directly from a flip-flop.
This more restrictive timing methodology makes top-level timing closure
easier.

The DCInput and DCOutput pair support a registered-output timing methodology.
When used, all decoupled inputs should be wrapped with a DCInput and all
decoupled outputs should be wrapped with DCOutput.  With this
all block outputs ("valid" and "bits" signals for output bundles, and "ready" for
input bundles) will come directly from a flop.

The DCFull module supports the registered-input-and-output methodology,
with the caveat that there may be 1-2 levels of logic on the "ready" signals.

The DCHold module can substitute for either a DCInput or DCOutput module.
It has superior timing characteristics, in that it closes timing on all three
elements of the bundle (valid, ready, and bits), but at a cost of reduced
throughput.

Note that while from a functionality standpoint all four of the compoonents 
look like single-entry or two-entry FIFOs, their underlying implementations are
very different to accomplish their different timing goals.

## Components

### DCInput

As noted above, the DCInput block is intended to be used on a block's decoupled
inputs.

### DCOutput

As noted above, the DCOutput block is intended to be used on a block's decoupled
outputs.

### DCFull

The DCFull implementation is simply a DCInput coupled with a DCOutput.
The combination of the two closes timing on all three elements of a Decoupled
bundle.

### DCHold

DCHold is a half-throughput component which is useful for timing closure on
interfaces which have low throughput, and for holding tokens until they are consumed
by some process.

### DCArbiter

This component is a round-robin arbiter with output storage.  It is similar in
functionality to RRArbiter.

### DCDemux

This is a decoupled "demultiplexer", in that it takes a token and a destination
port number and sends the token only to that destination port.

### DCMirror

DCMirror can be thought of as a superset of DCDemux functionality.  It
takes a token and sends one copy of that token to every output with a bit
set in the "dst" vector.  The mirror will block its input until all
output copies have been received, and allows output copies to be received in 
any order.

### DCReduce

The DCReduce module is a simple example showing how DC components can be used 
to create a decoupled block with registered-output timing.  The DCReduce block
takes a vector of up to N inputs and combines (reduces) them with a user-provided
operator function.  Note that the reduction operator does not guarentee a specific
ordering of how the reduction occurs, so the provided operator function should be
commutative or order-independent.

Because its inputs are all decoupled, the inputs can arrive in any order.
DCReduce will produce a result once all inputs have become valid, and
hold the result until it is acknowledged ("ready" is true).

## Functional Construction

DCInput, DCOutput, DCHold, and DCFull all support functional construction,
meaning you can wrap inputs and outputs
