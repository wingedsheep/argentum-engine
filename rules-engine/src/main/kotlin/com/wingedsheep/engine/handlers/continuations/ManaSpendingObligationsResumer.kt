package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ManaSpendingObligationsContinuation

class ManaSpendingObligationsResumer : AutoResumerModule {
    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(ManaSpendingObligationsContinuation::class) { state, frame, events, checkForMore ->
            if (frame.pendingIds.isNotEmpty()) ExecutionResult.error(state, "Each mana ability must contribute mana to the instruction")
            else checkForMore(state, events)
        }
    )
}
