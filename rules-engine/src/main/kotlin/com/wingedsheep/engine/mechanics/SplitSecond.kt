package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.SpellGrantedKeywordsComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Keyword

/**
 * Split second (CR 702.61) — the single read point for [Keyword.SPLIT_SECOND].
 *
 * "As long as this spell is on the stack, players can't cast other spells or activate abilities
 * that aren't mana abilities" (CR 702.61a). Mana abilities and special actions stay legal, and
 * triggered abilities trigger and are put on the stack as normal (CR 702.61b) — so the lock is a
 * restriction on *player actions*, not on the stack itself.
 *
 * Both halves of the offer/accept contract consult it:
 *  - [com.wingedsheep.engine.legalactions.LegalActionEnumerator] drops every locked offer, and
 *  - [com.wingedsheep.engine.core.ActionProcessor.validate] (plus `ActivationValidator` for
 *    [ActivateAbility], which alone knows whether the ability is a mana ability) rejects one.
 *
 * A spell has split second when it prints the keyword, when a one-shot grant stamped it onto that
 * spell ([SpellGrantedKeywordsComponent]), or when a permanent its controller controls grants it
 * through `GrantKeywordToOwnSpells` (Samut, Tyrant of Naktamun). A face-down spell has no
 * abilities (CR 708.2), so it never locks. Only spells count — an ability on the stack can't have
 * split second.
 */
object SplitSecond {

    const val REJECTION =
        "A spell with split second is on the stack — only mana abilities and special actions are allowed"

    /** True while at least one spell with split second is on the stack. */
    fun isLocked(state: GameState, cardRegistry: CardRegistry): Boolean {
        if (state.stack.isEmpty()) return false
        return state.stack.any { hasSplitSecond(state, cardRegistry, it) }
    }

    /**
     * Would the lock forbid [action]? Casting any spell and activating any non-mana ability —
     * cycling, typecycling, crew and saddle are activated abilities with their own action shapes.
     * [ActivateAbility] is answered by [isManaAbility], since only the ability lookup knows.
     * Everything else (special actions, decisions, passing, combat declarations) is untouched.
     */
    fun forbids(action: GameAction, isManaAbility: Boolean = false): Boolean = when (action) {
        is CastSpell, is CycleCard, is TypecycleCard, is CrewVehicle, is SaddleMount -> true
        is ActivateAbility -> !isManaAbility
        else -> false
    }

    private fun hasSplitSecond(
        state: GameState,
        cardRegistry: CardRegistry,
        stackObjectId: com.wingedsheep.sdk.model.EntityId
    ): Boolean {
        val container = state.getEntity(stackObjectId) ?: return false
        val spell = container.get<SpellOnStackComponent>() ?: return false
        if (container.has<FaceDownComponent>()) return false
        if (container.get<SpellGrantedKeywordsComponent>()?.keywords?.contains(Keyword.SPLIT_SECOND.name) == true) {
            return true
        }
        val cardDef = container.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return false
        val controllerId = container.get<ControllerComponent>()?.playerId ?: spell.casterId
        return GrantedKeywordResolver(cardRegistry).hasKeyword(state, controllerId, cardDef, Keyword.SPLIT_SECOND, stackObjectId)
    }
}
