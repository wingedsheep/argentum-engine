package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.CommanderZoneChoiceContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderZoneChoiceAskedComponent
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone

/**
 * CR 903.9a — if a commander is in a graveyard or in exile (and, by the modern Commander
 * rules committee extension, in its owner's hand or library) and that object was put into
 * that zone since the last time state-based actions were checked, its owner may put it
 * into the command zone.
 *
 * Implementation: when the format enables commanders *without* the
 * [Format.alwaysDivertToCommand] shortcut, pause the SBA loop with a yes/no
 * decision the first time the SBA sees a given commander outside the command zone. After
 * the prompt (yes or no) we attach [CommanderZoneChoiceAskedComponent] so the SBA does
 * not re-ask on the next iteration; [ZoneTransitionService.moveToZone] strips that marker
 * whenever the commander next changes zones, restoring the "fresh question on next entry"
 * semantics of "since the last time state-based actions were checked".
 *
 * Only one commander is asked about per SBA pass — APNAP-by-turn-order is preserved by the
 * outer loop in [com.wingedsheep.engine.mechanics.StateBasedActionChecker.checkAndApply].
 */
class CommanderZoneChoiceCheck(
    private val decisionHandler: DecisionHandler
) : StateBasedActionCheck {
    override val name = "903.9a Commander Zone Choice"
    override val order = SbaOrder.COMMANDER_ZONE_CHOICE

    override fun check(state: GameState): ExecutionResult {
        val format = state.format
        if (!format.usesCommanders) return ExecutionResult.success(state)
        // alwaysDivertToCommand sends the commander to the command zone synchronously via the
        // zone-change replacement, so it never sits in a non-command zone for this SBA to see.
        if (format.alwaysDivertToCommand) return ExecutionResult.success(state)

        for (playerId in state.turnOrder) {
            for ((entityId, commander) in state.findEntitiesWith<CommanderComponent>()) {
                if (commander.ownerId != playerId) continue
                val container = state.getEntity(entityId) ?: continue
                if (container.has<CommanderZoneChoiceAskedComponent>()) continue

                val zoneKey = state.zones.entries.firstOrNull { entityId in it.value }?.key ?: continue
                if (zoneKey.zoneType !in CHOICE_ZONES) continue

                val cardName = container.get<CardComponent>()?.name ?: "your commander"
                val zoneLabel = zoneLabelFor(zoneKey.zoneType)

                val decisionResult = decisionHandler.createYesNoDecision(
                    state = state,
                    playerId = playerId,
                    sourceId = entityId,
                    sourceName = cardName,
                    prompt = "Put $cardName into the command zone instead of leaving it in $zoneLabel?",
                    yesText = "Command zone",
                    noText = "Leave in $zoneLabel",
                    phase = DecisionPhase.STATE_BASED
                )

                val continuation = CommanderZoneChoiceContinuation(
                    decisionId = decisionResult.pendingDecision!!.id,
                    commanderId = entityId,
                    ownerId = playerId,
                    currentZone = zoneKey.zoneType
                )

                val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

                return ExecutionResult.paused(
                    stateWithContinuation,
                    decisionResult.pendingDecision,
                    decisionResult.events
                )
            }
        }

        return ExecutionResult.success(state)
    }

    companion object {
        private val CHOICE_ZONES = setOf(
            Zone.GRAVEYARD,
            Zone.EXILE,
            Zone.HAND,
            Zone.LIBRARY,
        )

        private fun zoneLabelFor(zone: Zone): String = when (zone) {
            Zone.GRAVEYARD -> "the graveyard"
            Zone.EXILE -> "exile"
            Zone.HAND -> "your hand"
            Zone.LIBRARY -> "your library"
            else -> zone.displayName
        }
    }
}
