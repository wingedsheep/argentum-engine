package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Sneak timing + cost helper (CR 702.190).
 *
 * "Sneak [cost]" means: *"Any time you could cast an instant during your declare blockers
 * step, you may cast this spell by paying [cost] and returning an unblocked creature you
 * control to its owner's hand rather than paying this spell's mana cost."* (CR 702.190a)
 *
 * This object centralizes the two facts every sneak code path needs so the gnarly combat-state
 * reads live in exactly one place (the legal-action enumerator, the cast handler's validate and
 * execute all consult it):
 *  - [isWindowOpen] — is it legal to *announce* a sneak cast right now, and
 *  - [unblockedAttackers] — which creatures can be returned to pay the sneak cost.
 *
 * Both are pure reads against [GameState]; the controller of each attacker is taken from the
 * projected state (battlefield reads must honor control-changing effects, CR 613).
 */
object SneakWindow {

    /**
     * The window is open for [playerId] when it is the declare blockers step of *their* combat
     * (they are the active player, CR 702.190a "your declare blockers step"), the declare-blockers
     * turn-based action has happened (CR 509.1h assigns blocked/unblocked status only then — before
     * it no attacker is "unblocked"), and they control at least one unblocked attacker to return.
     */
    fun isWindowOpen(state: GameState, playerId: EntityId): Boolean =
        state.step == Step.DECLARE_BLOCKERS &&
            state.activePlayerId == playerId &&
            blockersDeclared(state, playerId) &&
            unblockedAttackers(state, playerId).isNotEmpty()

    /** The defending player has performed the declare-blockers turn-based action this combat. */
    private fun blockersDeclared(state: GameState, playerId: EntityId): Boolean =
        state.getOpponents(playerId).any { defender ->
            state.getEntity(defender)?.get<BlockersDeclaredThisCombatComponent>() != null
        }

    /**
     * Unblocked attackers [playerId] controls — the legal pool for the "return an unblocked
     * creature you control to its owner's hand" portion of a sneak cost. A creature qualifies
     * when it has an [AttackingComponent], no [BlockedComponent], and its projected controller
     * is [playerId]. Blocked status is sticky (CR 509.1h: a creature remains blocked even if
     * all the creatures blocking it are removed from combat), which is exactly what
     * [BlockedComponent] encodes — it survives its blockers leaving combat
     * (see [com.wingedsheep.engine.mechanics.combat.CombatRemovalHelper]), so it must be read
     * here instead of scanning the current blockers.
     */
    fun unblockedAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield().filter { entityId ->
            val entity = state.getEntity(entityId) ?: return@filter false
            entity.get<AttackingComponent>() != null &&
                entity.get<BlockedComponent>() == null &&
                projected.getController(entityId) == playerId
        }
    }

    /**
     * The granted sneak cost from any active `GraveyardCreaturesHaveSneak` static the player
     * controls (Ninja Teen level 3: "Creature cards in your graveyard have sneak {3}{B}"), or null.
     * Read from each permanent's active class-level static abilities so an inactive class level
     * doesn't grant it.
     */
    fun graveyardSneakGrantCost(
        state: GameState,
        playerId: EntityId,
        cardRegistry: com.wingedsheep.engine.registry.CardRegistry
    ): com.wingedsheep.sdk.core.ManaCost? {
        for (permId in state.getBattlefield()) {
            val container = state.getEntity(permId) ?: continue
            // Projected controller — a control-changing effect on the granting permanent must move
            // the grant with it (CR 613), consistent with unblockedAttackers above.
            if (state.projectedState.getController(permId) != playerId) continue
            val defId = container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.cardDefinitionId ?: continue
            val def = cardRegistry.getCard(defId) ?: continue
            val classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            def.script.effectiveStaticAbilities(classLevel)
                .filterIsInstance<com.wingedsheep.sdk.scripting.GraveyardCreaturesHaveSneak>()
                .firstOrNull()?.let { return it.cost }
        }
        return null
    }

    /**
     * The effective sneak cost for casting [cardId] (definition [cardDef]) by [playerId]: the
     * printed Sneak cost, or — for a creature card in the player's graveyard while they control an
     * active `GraveyardCreaturesHaveSneak` grant — the granted cost. Null when no sneak applies.
     */
    fun effectiveSneakCost(
        state: GameState,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        cardId: EntityId,
        playerId: EntityId,
        cardRegistry: com.wingedsheep.engine.registry.CardRegistry
    ): com.wingedsheep.sdk.core.ManaCost? {
        // Printed Sneak or Ninjutsu — both expose the cost via `ninjutsuStyleCost` (the engine
        // calls the whole declare-blockers mechanic family "sneak" for historical reasons; Ninjutsu
        // is the canonical rules name, Sneak its TMNT reflavor).
        cardDef.keywordAbilities
            .firstNotNullOfOrNull { it.ninjutsuStyleCost }
            ?.let { return it }
        if (cardDef.typeLine.isCreature && cardId in state.getGraveyard(playerId)) {
            return graveyardSneakGrantCost(state, playerId, cardRegistry)
        }
        return null
    }
}
