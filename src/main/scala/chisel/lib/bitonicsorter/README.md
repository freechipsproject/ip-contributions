# Bitonic Sorter

## Background

Hardware based sorting is generally nontrivial and exceptionally resource intensive.
This package provides generic sort combinational support to Vec.
It does not contains state or memory.
The size of the generated module grows rapidly with increasing element size and the number of elements.
With that in mind, use caution.
There are some use cases where Sort would be nice to have.

## Usage
The generator creates a module that combinational sorts an input Vec immediately into an output Vec.
Check out the unit tests for examples of different kinds of elements, comparison functions and sizes.

