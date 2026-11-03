package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.CreateRandomCreatureTokenWithManaValueEffect
import kotlin.reflect.KClass

/**
 * Executor for [CreateRandomCreatureTokenWithManaValueEffect] — the Momir Basic avatar payoff.
 *
 * Composes three atomic steps: gather the candidate creatures, select one at random with the
 * game's seeded RNG, and mint a token copy of it.
 *
 *  1. **Gather** — read the set-scoped eligible-creature pool from the active
 *     [Format.MomirBasic.eligibleCreatureNames] and filter to creatures whose mana value equals
 *     the resolved [CreateRandomCreatureTokenWithManaValueEffect.manaValue]. The list is *filtered*
 *     (not re-collected from the registry, whose map order is unspecified) and re-sorted by name so
 *     the candidate order is deterministic regardless of how the pool was supplied — a precondition
 *     for replay-stable RNG.
 *
 *     Creatures with genuinely **no mana cost** ([CardDefinition.hasNoManaCost]) are dropped, and
 *     dropped here rather than only at the pool's source: the pool is opaque data supplied from
 *     outside the engine, so this is the only place the exclusion can be guaranteed. A card with
 *     no mana cost has mana value 0 (CR 202.1b — no mana cost is an *unpayable* cost, CR 118.6),
 *     which silently lands it in the X=0 bucket even though it is not a card anybody could ever
 *     cast: in practice these are the meld results (CR 701.42) such as
 *     Hanweir, the Writhing Township, which exist in the corpus but are never real deck cards.
 *     "{0}" is a *printed*, payable cost and stays eligible (Ornithopter, Memnite).
 *  2. **Select** — `GameRng.pick` over the sorted candidates, threading the advanced RNG back onto
 *     the state. If no creature has that mana value, nothing happens (the activation cost was still
 *     paid — CR: an effect that can't do anything does nothing).
 *  3. **Mint** — [TokenFromDefinition.mint] puts a token copy of the chosen definition onto the
 *     controller's battlefield.
 */
class CreateRandomCreatureTokenWithManaValueExecutor(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry
) : EffectExecutor<CreateRandomCreatureTokenWithManaValueEffect> {

    override val effectType: KClass<CreateRandomCreatureTokenWithManaValueEffect> =
        CreateRandomCreatureTokenWithManaValueEffect::class

    override fun execute(
        state: GameState,
        effect: CreateRandomCreatureTokenWithManaValueEffect,
        context: EffectContext
    ): EffectResult {
        val manaValue = amountEvaluator.evaluate(state, effect.manaValue, context)

        // The candidate pool is the active Momir/Vanguard format's set-scoped creature list.
        // Outside that format there is no pool, so the effect does nothing.
        val pool = (state.format as? Format.MomirBasic)?.eligibleCreatureNames ?: emptyList()

        val candidates = pool
            .mapNotNull { cardRegistry.getCard(it) }
            .filter { it.isCreature && !it.hasNoManaCost && it.cmc == manaValue }
            .sortedBy { it.name }

        if (candidates.isEmpty()) {
            // No creature of that mana value — cost already paid, nothing to create (CR 608.2g).
            return EffectResult.success(state)
        }

        // "Created under your control instead" (Crafty Cutpurse) before any other token
        // replacement — asked, if at all, before the random pick so the re-run draws the same card.
        val tokenControllerId = when (val redirect = TokenCreationReplacementHelper.redirectController(
            state, context.controllerId, context, effect, amountEvaluator.predicates
        )) {
            is TokenControllerRedirect.Resolved -> redirect.controllerId
            is TokenControllerRedirect.Ask -> return redirect.result
        }

        val (chosen, stateAfterPick) = state.nextRandom { pick(candidates) }

        // "If one or more tokens would be created under your control, that many … are created
        // instead" (Holo-Engineer). A question about the substitute pauses on the state *before*
        // the pick, so the re-run after the answer draws the same creature from the same seed.
        val prospective = com.wingedsheep.engine.core.CardEntityFactory.create(chosen, ownerId = tokenControllerId)
            .get<com.wingedsheep.engine.state.components.identity.CardComponent>()
        if (prospective != null) {
            TokenCreationReplacementHelper.substituteIfReplaced(
                stateAfterPick, prospective, tokenControllerId, 1, context, effect,
                amountEvaluator.predicates, staticAbilityHandler, cardRegistry,
                pauseState = state,
            )?.let { return it }
        }

        return TokenFromDefinition.mint(
            state = stateAfterPick,
            cardDef = chosen,
            controllerId = tokenControllerId,
            cardRegistry = cardRegistry,
            staticAbilityHandler = staticAbilityHandler,
            predicateEvaluator = amountEvaluator.predicates
        )
    }
}
