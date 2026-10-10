package com.wingedsheep.ai.engine

import com.wingedsheep.ai.arena.harnessRegistry
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.fail
import kotlin.random.Random

/**
 * What the client is offered is what the server accepts.
 *
 * The legal-action enumerators and the action handlers' `validate` share one legality kernel
 * ([com.wingedsheep.engine.legality.LegalityKernel]) for restrictions and permissions, but the
 * enumerators still assemble each offer themselves. This test closes the loop: it plays seeded
 * random games over a spread of sets and, at every priority point, submits each affordable offer
 * that needs no further input straight to [ActionProcessor.validate]. An offer the handler then
 * refuses is a legality check that exists on one side only.
 *
 * "Needs no further input" means the offer's bare action is complete as enumerated: no targets,
 * no X, no additional-cost choice, no modes, no damage division, and no alternative payment
 * (convoke, delve, harmonize, tap-for-generic) — the enumerator counts those toward affordability,
 * but the client supplies them on the action. Those offers carry a choice the client makes before
 * submitting, so their bare action is legitimately incomplete.
 */
class LegalActionsPassValidateTest : FunSpec({

    for ((setCode, seed) in SETS) {
        test("every complete affordable offer passes validate ($setCode)") {
            val set = MtgSetCatalog.requireByCode(setCode)
            val registry = harnessRegistry(set)
            val rng = Random(seed)
            val refusals = sortedSetOf<String>()
            repeat(GAMES_PER_SET) {
                val deck1 = buildRandomSealedDeck(set.cards, rng)
                val deck2 = buildRandomSealedDeck(set.cards, rng)
                playCheckingOffers(registry, deck1, deck2, rng, refusals)
            }
            if (refusals.isNotEmpty()) {
                fail("Offered, then refused by validate:\n" + refusals.joinToString("\n"))
            }
        }
    }
}) {
    companion object {
        /** Sets chosen for activated-ability, alternative-zone and linked-exile variety. */
        private val SETS = listOf(
            "POR" to 1L,
            "MRD" to 2L,
            "RAV" to 3L,
            "LRW" to 4L,
            "BLB" to 5L,
            "DSK" to 6L,
        )
        private const val GAMES_PER_SET = 4
        private const val MAX_TURNS = 30
        private const val MAX_ACTIONS = 1_500

        private fun isComplete(offer: LegalAction): Boolean =
            offer.affordable &&
                (offer.action is ActivateAbility || offer.action is CastSpell || offer.action is PlayLand) &&
                !offer.requiresTargets &&
                offer.targetRequirements.isNullOrEmpty() &&
                !offer.hasXCost &&
                offer.additionalCostInfo == null &&
                offer.modalEnumeration == null &&
                !offer.requiresDamageDistribution &&
                !offer.hasConvoke &&
                !offer.hasDelve &&
                !offer.hasHarmonize &&
                !offer.hasTapForGeneric &&
                !offer.tapForPower &&
                !offer.requiresForage &&
                offer.additionalLifeCost == 0

        private fun playCheckingOffers(
            registry: CardRegistry,
            deck1: com.wingedsheep.sdk.model.Deck,
            deck2: com.wingedsheep.sdk.model.Deck,
            rng: Random,
            refusals: MutableSet<String>,
        ) {
            val processor = ActionProcessor(registry)
            val enumerator = LegalActionEnumerator.create(registry)
            var state: GameState = GameInitializer(registry).initializeGame(
                GameConfig(
                    players = listOf(PlayerConfig("P1", deck1), PlayerConfig("P2", deck2)),
                    skipMulligans = true,
                    startingPlayerIndex = 0,
                    seed = rng.nextLong(),
                )
            ).state

            var actions = 0
            while (!state.gameOver && state.turnNumber < MAX_TURNS && actions < MAX_ACTIONS) {
                actions++
                val pending = state.pendingDecision
                val action = if (pending != null) {
                    SubmitDecision(pending.playerId, randomDecisionResponse(pending, rng))
                } else {
                    val player = state.priorityPlayerId ?: break
                    val offers = enumerator.enumerate(state, player)
                    for (offer in offers.filter(::isComplete)) {
                        val refusal = processor.validate(state, offer.action) ?: continue
                        refusals += "${offer.actionType} \"${offer.description}\" (step ${state.step}): $refusal"
                    }
                    val affordable = offers.filter { it.affordable }
                    if (affordable.isEmpty()) break
                    affordable[rng.nextInt(affordable.size)].action
                }
                val result = processor.process(state, action).result
                state = if (result.error == null) {
                    result.state
                } else {
                    processor.process(state, PassPriority(action.playerId)).result
                        .takeIf { it.error == null }?.state ?: break
                }
            }
        }
    }
}
