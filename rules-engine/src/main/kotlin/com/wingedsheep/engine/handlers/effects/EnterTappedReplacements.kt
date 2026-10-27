package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.replacement.ActiveReplacements
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Applies "[filter] enter the battlefield tapped" replacement effects sourced from *other*
 * battlefield permanents (e.g. Zhao, the Moon Slayer's "Nonbasic lands enter tapped"), emblems,
 * and durational grants ([ActiveReplacements] enumerates all three; "you" is the grant's
 * controller, so a resolved sorcery's "lands you control enter tapped this turn" outlives it).
 *
 * The global counterpart of the self-only [com.wingedsheep.sdk.scripting.EntersTapped]: a
 * [PermanentsEnterTapped] is stamped into the source's [com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent]
 * (see `StaticAbilityHandler.isRuntimeReplacementEffect`) and consulted from the battlefield
 * whenever some *other* permanent enters. The entry paths (PlayLandHandler,
 * ZoneTransitionService) ask [entersTapped] and mark the permanent tapped when it returns true.
 *
 * Symmetric to [EnterUntappedReplacements]; per CR 614 an applicable [EnterUntappedReplacements]
 * wins, so callers consult that first and only apply this tap when the entering permanent is not
 * already made untapped by a replacement.
 */
object EnterTappedReplacements {

    /**
     * True if any [PermanentsEnterTapped] replacement in force — printed on a battlefield
     * permanent, held by an emblem, or granted for a duration by `Effects.GrantReplacementEffect`
     * (Nahiri's Lithoforming: "Lands you control enter tapped this turn") — has an
     * `appliesTo` filter that matches [enteringEntityId] (controlled by [enteringControllerId]) and
     * whose optional `condition` gate holds. The entering entity must already carry its
     * [ControllerComponent] /
     * [com.wingedsheep.engine.state.components.identity.CardComponent] so the filter
     * (type/subtype/"you control") resolves correctly.
     */
    fun entersTapped(
        state: GameState,
        enteringEntityId: EntityId,
        enteringControllerId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        for (active in ActiveReplacements.all(state) { it is PermanentsEnterTapped }) {
            val effect = active.effect as? PermanentsEnterTapped ?: continue
            if (!active.granted && active.sourceId == enteringEntityId) continue
            if (!matchesEnterFilter(effect.appliesTo, enteringEntityId, active.sourceId, active.controllerId, state, predicateEvaluator = predicateEvaluator)) continue
            // Optional gate evaluated against the replacement *source*, mirroring
            // RedirectDamage.condition — e.g. Ashling's Prerogative's tap clause applies only
            // while the mode it chose as it entered is the one this effect was written for.
            val gate = effect.condition
            if (gate != null) {
                val gateContext = EffectContext(sourceId = active.sourceId, controllerId = active.controllerId)
                if (!predicateEvaluator.conditions.evaluate(state, gate, gateContext)) continue
            }
            return true
        }
        return false
    }

    /**
     * Resolve global "[filter] enter tapped/untapped" replacements for a token [tokenId] just
     * placed onto [controllerId]'s battlefield, mirroring the entry-tap resolution in
     * [ZoneTransitionService]. Tokens are permanents entering the battlefield, so they honor
     * Dauntless Dismantler / Authority of the Consuls-style effects just like cast or played
     * permanents — but [com.wingedsheep.engine.handlers.effects.BattlefieldEntry.place] (the
     * ad-hoc insertion path token executors use) deliberately doesn't set tapped state, so each
     * token-minting site calls this immediately after placing the token.
     *
     * [definedTapped] is whether the token was minted already tapped (its own `effect.tapped` /
     * self-`EntersTapped`); [attacking] whether it entered attacking (a combat token keeps its
     * tapped state and is never flipped untapped). Per CR 614 an applicable "enters untapped"
     * replacement wins over a global "enters tapped".
     *
     * The token must already carry its `ControllerComponent` / `CardComponent` so the replacement
     * filters ("artifacts your opponents control", …) resolve.
     */
    fun applyCreatedTokenEntryTap(
        state: GameState,
        tokenId: EntityId,
        controllerId: EntityId,
        definedTapped: Boolean = false,
        attacking: Boolean = false,
        predicateEvaluator: PredicateEvaluator
    ): GameState {
        val entersUntapped = EnterUntappedReplacements.entersUntapped(state, tokenId, controllerId, predicateEvaluator = predicateEvaluator)
        return when {
            definedTapped && !attacking && entersUntapped ->
                state.updateEntity(tokenId) { it.without<TappedComponent>() }
            !definedTapped && !entersUntapped &&
                entersTapped(state, tokenId, controllerId, predicateEvaluator = predicateEvaluator) ->
                state.updateEntity(tokenId) { it.with(TappedComponent) }
            else -> state
        }
    }

    /**
     * [replacementSourceId] is the permanent that *grants* the replacement, not the one entering —
     * the entering permanent is what the filter is evaluated against and is passed separately. The
     * distinction only shows up for a source-relative predicate: "creatures enchanted player
     * controls" has to read the granting Aura's own attachment, and would otherwise be asked about
     * the entering creature's.
     */
    private fun matchesEnterFilter(
        event: EventPattern,
        enteringEntityId: EntityId,
        replacementSourceId: EntityId,
        sourceControllerId: EntityId,
        state: GameState,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        if (event !is EventPattern.ZoneChangeEvent) return false
        if (event.to != Zone.BATTLEFIELD) return false
        val predicateContext = PredicateContext(
            sourceId = replacementSourceId,
            controllerId = sourceControllerId,
        )
        return predicateEvaluator.matches(
            state, state.projectedState, enteringEntityId, event.filter, predicateContext
        )
    }
}
