package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction

class PassPriorityEnumerator : ActionEnumerator {
    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        // A teammate who already passed this round (CR 805.5 team priority) has nothing to pass
        // again until someone acts and re-arms the round — mirror PassPriorityHandler's gate so the
        // client is never offered an action the engine will refuse. Never true for the baton holder.
        if (context.playerId in state.priorityPassedBy && context.playerId != state.priorityPlayerId) {
            return emptyList()
        }
        return listOf(
            LegalAction(
                action = PassPriority(context.playerId),
                actionType = "PassPriority",
                description = "Pass priority"
            )
        )
    }
}
