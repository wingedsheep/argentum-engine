package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantWebSlingingToSpells
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Web-slinging cost helper (CR 702.188, Marvel's Spider-Man).
 *
 * "Web-slinging [cost]" means: *"You may cast this spell by paying [cost] and returning a tapped
 * creature you control to its owner's hand rather than paying its mana cost."* (CR 702.188a)
 *
 * Unlike the ninjutsu family ([SneakWindow]), web-slinging grants no timing permission — the spell
 * is cast at its normal timing — so there is no "window" to check. The one fact every web-slinging
 * code path needs is the pool of creatures that can be returned to pay the cost, centralized here so
 * the legal-action enumerator, the cast handler's validate, and its execute all agree:
 *  - [tappedCreaturesYouControl] — which creatures can be returned.
 *
 * Reads are against [GameState]; the controller and creature-ness of each permanent come from the
 * projected state (battlefield reads must honor control-changing / type-changing effects, CR 613),
 * while tapped status is a real base-state fact ([TappedComponent]).
 */
object WebSlinging {

    /** The printed web-slinging ability on [cardDef], or null. */
    fun webSlingingAbility(cardDef: CardDefinition): KeywordAbility.WebSlinging? =
        cardDef.keywordAbilities.filterIsInstance<KeywordAbility.WebSlinging>().firstOrNull()

    /**
     * The effective web-slinging ability for the spell [spellCardId] (currently in [controllerId]'s
     * hand), or null. A printed web-slinging on [cardDef] wins; otherwise, when
     * [controllerId]/[cardRegistry]/[predicateEvaluator] are supplied, a battlefield
     * [GrantWebSlingingToSpells] static controlled by [controllerId] whose `spellFilter` matches the
     * spell grants web-slinging at the static's cost. Amazing Spider-Man: "Each legendary spell you
     * cast that's one or more colors has web-slinging {G}{W}{U}." Mirrors `FlashbackGrants`.
     */
    fun effectiveWebSlinging(
        state: GameState,
        spellCardId: EntityId,
        cardDef: CardDefinition?,
        controllerId: EntityId? = null,
        cardRegistry: CardRegistry? = null,
        predicateEvaluator: PredicateEvaluator? = null,
    ): KeywordAbility.WebSlinging? {
        cardDef?.let { webSlingingAbility(it) }?.let { return it }

        if (controllerId != null && cardRegistry != null && predicateEvaluator != null) {
            val context = PredicateContext(controllerId = controllerId)
            for (granterId in state.controlledBattlefield(controllerId)) {
                val def = state.getEntity(granterId)?.get<CardComponent>()
                    ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
                for (ability in def.script.staticAbilities) {
                    if (ability !is GrantWebSlingingToSpells) continue
                    if (predicateEvaluator.matches(state, state.projectedState, spellCardId, ability.spellFilter, context)) {
                        return KeywordAbility.WebSlinging(ability.cost)
                    }
                }
            }
        }
        return null
    }

    /**
     * The tapped creatures [playerId] controls — the legal pool for the "return a tapped creature
     * you control to its owner's hand" portion of a web-slinging cost (CR 702.188a). A permanent
     * qualifies when its projected controller is [playerId], it is projected as a creature, and it
     * is tapped.
     */
    fun tappedCreaturesYouControl(state: GameState, playerId: EntityId): List<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield().filter { entityId ->
            projected.getController(entityId) == playerId &&
                projected.isCreature(entityId) &&
                state.getEntity(entityId)?.has<TappedComponent>() == true
        }
    }
}
