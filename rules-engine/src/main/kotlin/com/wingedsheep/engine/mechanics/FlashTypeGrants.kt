package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.FlashGrantsThisTurnComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * The single source of truth for "may this card be cast as though it had flash?" — every source of
 * flash *other than* the printed [com.wingedsheep.sdk.core.Keyword.FLASH] keyword.
 *
 * Two read sites need the same answer and must never drift apart:
 *  - `CastPermissionUtils.hasGrantedFlash`, consulted while enumerating legal actions, and
 *  - `CastZoneResolver.hasGrantedFlash`, which authoritatively re-checks at cast time.
 *
 * They live in different packages and neither can see the other's private helpers, so the whole
 * decision lives here rather than being written twice. Both delegate; neither keeps a copy.
 *
 * Three sources are consulted, in order:
 *
 * 1. **The card's own `conditionalFlash`** — "this spell has flash as long as …" printed on the
 *    card itself (ferocious and friends), evaluated in the caster's context.
 * 2. **Turn-scoped player grants** — `GrantFlashToSpellsEffect` writes
 *    [FlashGrantsThisTurnComponent] filters onto the caster (Borne Upon a Wind).
 * 3. **[GrantFlashToSpellType] statics on battlefield permanents** — Quick Sliver, Raff Capashen,
 *    Radagast of Rhosgobel, Captain Mar-Vell.
 *
 * ### Conditional grants ("as long as …" — the grant applies only while its condition holds)
 *
 * A battlefield grant may be wrapped in a [ConditionalStaticAbility], which is what
 * `staticAbility { condition = … }` produces for "**As long as** an opponent has cast a spell this
 * turn, you may cast spells as though they had flash" (Captain Mar-Vell, Space-Born). Flash
 * permission is read at cast-legality time and never reaches the layer system — `StaticAbilityHandler`
 * lists [GrantFlashToSpellType] among the abilities it deliberately lowers to no continuous effect —
 * so the wrapper has to be unwrapped *here* or the grant is silently inert rather than conditional.
 * [activeGrant] does that unwrapping, evaluating the gate against the granting permanent. An
 * unconditional grant passes through unchanged, so Quick Sliver et al. are unaffected.
 *
 * The condition is evaluated with the granting permanent as the source and its **controller** as
 * `you` (CR 109.5) — the same player the [GrantFlashToSpellType.controllerOnly] check uses, so both
 * gates always agree about who the granter's controller is, and a stolen granter switches sides for
 * both at once.
 */
object FlashTypeGrants {

    /**
     * Whether [spellCardId] may currently be cast as though it had flash by something other than a
     * printed flash keyword. Callers `||` this with their own printed-keyword check.
     * [casterId] is explicit because the player casting a card may differ from its owner.
     */
    fun hasGrantedFlash(
        state: GameState,
        spellCardId: EntityId,
        casterId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        conditionEvaluator: ConditionEvaluator,
    ): Boolean {
        // 1. The card's own conditionalFlash (e.g. Ferocious).
        val spellDef = state.getEntity(spellCardId)?.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val conditionalFlash = spellDef?.script?.conditionalFlash
        if (conditionalFlash != null) {
            val effectContext = EffectContext(sourceId = spellCardId, controllerId = casterId)
            if (conditionEvaluator.evaluate(state, conditionalFlash, effectContext)) return true
        }

        val context = PredicateContext(controllerId = casterId)

        // 2. Turn-scoped grants on the caster (Borne Upon a Wind etc., via
        // GrantFlashToSpellsEffect → FlashGrantsThisTurnComponent).
        val turnGrants = state.getEntity(casterId)?.get<FlashGrantsThisTurnComponent>()
        if (turnGrants != null) {
            for (filter in turnGrants.filters) {
                if (predicateEvaluator.matches(state, state.projectedState, spellCardId, filter, context)) {
                    return true
                }
            }
        }

        // 3. GrantFlashToSpellType statics on battlefield permanents (every player's battlefield).
        // Controlled view, not the ownership-keyed zone map: "you" in an ability is the object's
        // controller (CR 109.5), so a stolen granter must grant to its new controller, and
        // `controlledBattlefield` also drops phased-out permanents (CR 702.26b — a phased-out
        // permanent "is treated as though it does not exist").
        for (playerId in state.turnOrder) {
            for (entityId in state.controlledBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val classLevel = state.getEntity(entityId)?.get<ClassLevelComponent>()?.currentLevel
                for (raw in cardDef.script.effectiveStaticAbilities(classLevel)) {
                    val ability = activeGrant(state, raw, entityId, playerId, conditionEvaluator)
                        ?: continue
                    // If controllerOnly, only the permanent's controller benefits.
                    if (ability.controllerOnly && playerId != casterId) continue
                    // "The first [type] spell you cast each turn" — the grant covers only one
                    // spell per turn (Radagast of Rhosgobel).
                    if (!nthGateAllows(state, casterId, ability, predicateEvaluator)) continue
                    if (predicateEvaluator.matches(state, state.projectedState, spellCardId, ability.filter, context)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * The [GrantFlashToSpellType] carried by [raw] when it is currently granting, else null.
     *
     * A bare grant passes through. A grant wrapped in a [ConditionalStaticAbility] is honored only
     * while its condition holds against the granting permanent [sourceId], controlled by
     * [granterController]. Anything else (a different static ability, or a conditional wrapping one)
     * is not a flash grant and yields null.
     */
    private fun activeGrant(
        state: GameState,
        raw: StaticAbility,
        sourceId: EntityId,
        granterController: EntityId,
        conditionEvaluator: ConditionEvaluator,
    ): GrantFlashToSpellType? = when (raw) {
        is GrantFlashToSpellType -> raw
        is ConditionalStaticAbility -> {
            val inner = raw.ability as? GrantFlashToSpellType
            if (inner == null) {
                null
            } else {
                val context = EffectContext(sourceId = sourceId, controllerId = granterController)
                if (conditionEvaluator.evaluate(state, raw.condition, context)) inner else null
            }
        }
        else -> null
    }

    /**
     * Whether [ability]'s per-turn gate (if any) currently lets [casterId] cast a matching spell at
     * instant speed. Always true for an ungated grant (Quick Sliver, Raff Capashen).
     *
     * The count comes off `GameState.spellsCastThisTurnByPlayer`, the same record
     * `CostGating.NthOfTypePerTurn` uses in `CostCalculator`, and with the same convention: the spell
     * being cast is **not** yet in the list, so the gate is open exactly while the caster has already
     * cast `n - 1` matching spells this turn. That means a matching spell already cast this turn
     * closes the window even if it was countered or fizzled — matching the "you cast" wording, which
     * cares about the cast and not the resolution.
     */
    private fun nthGateAllows(
        state: GameState,
        casterId: EntityId,
        ability: GrantFlashToSpellType,
        predicateEvaluator: PredicateEvaluator,
    ): Boolean {
        val n = ability.nthOfTypePerTurn ?: return true
        val castThisTurn = state.spellsCastThisTurnByPlayer[casterId] ?: emptyList()
        return castThisTurn.count { predicateEvaluator.matchesFilter(it, ability.filter) } == n - 1
    }
}
