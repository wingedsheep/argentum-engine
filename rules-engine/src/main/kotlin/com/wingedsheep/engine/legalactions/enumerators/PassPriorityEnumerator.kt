package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction

class PassPriorityEnumerator : ActionEnumerator {
    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        return listOf(
            LegalAction(
                action = PassPriority(context.playerId),
                actionType = "PassPriority",
                description = "Pass priority"
            )
        )
    }
}
