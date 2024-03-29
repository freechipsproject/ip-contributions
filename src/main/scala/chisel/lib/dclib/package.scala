package chisel.lib

import chisel3._

package object dclib {
  // This provides a default function to create a double synchronizer from two basic flops
  // It is expected the user will override this with the correct logic to create a synchronizer
  // their process technology
  def defaultDoubleSync(x: UInt): UInt = {
    RegNext(RegNext(x))
  }
}
