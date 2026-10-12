package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.PutCompanionIntoHand
import com.wingedsheep.engine.handlers.actions.ability.PutCompanionIntoHandHandler
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.companionOutsideGame

/**
 * Enumerates the companion special action (CR 702.139a, 116.2g): pay {3} to put your revealed
 * companion from outside the game into your hand.
 *
 * Offered at sorcery timing — your main phase, empty stack — and only while the
 * companion is still waiting outside the game and the once-per-game action hasn't been taken
 * ([companionOutsideGame] answers both). Like plot and foretell, it is listed even when the {3}
 * can't be paid, marked unaffordable.
 */
class CompanionEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId
        // The handler's window exactly (CR 116.2g) — not canPlaySorcerySpeed, which a forced
        // play also opens.
        if (!state.step.isMainPhase || state.stack.isNotEmpty() || !state.isActiveTurnFor(playerId)) {
            return emptyList()
        }
        val companionId = state.companionOutsideGame(playerId) ?: return emptyList()
        val name = state.getEntity(companionId)?.get<CardComponent>()?.name ?: return emptyList()
        val cost = PutCompanionIntoHandHandler.COST

        val affordable = context.manaSolver.canPay(
            state, playerId, cost, precomputedSources = context.availableManaSources
        )
        val autoTapPreview = if (context.skipAutoTapPreview) null else {
            context.manaSolver.solve(
                state, playerId, cost, precomputedSources = context.availableManaSources
            )?.sources?.map { it.entityId }
        }
        return listOf(
            LegalAction(
                actionType = "PutCompanionIntoHand",
                description = "Put companion $name into your hand",
                action = PutCompanionIntoHand(playerId, companionId),
                affordable = affordable,
                manaCostString = cost.toString(),
                autoTapPreview = autoTapPreview
            )
        )
    }
}
