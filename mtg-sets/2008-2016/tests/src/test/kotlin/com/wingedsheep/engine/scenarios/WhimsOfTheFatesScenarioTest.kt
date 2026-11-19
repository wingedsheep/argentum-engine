package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bng.cards.WhimsOfTheFates
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Whims of the Fates — {5}{R} Sorcery.
 *
 * Starting with you, each player separates all permanents they control into three piles. Then
 * each player chooses one of their piles at random and sacrifices those permanents.
 */
class WhimsOfTheFatesScenarioTest : ScenarioTestBase() {

    private fun build(seed: Long) = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Whims of the Fates")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .withRngSeed(seed)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Plays one game through: the caster splits Lions | Courser | the rest, the opponent piles everything together. */
    private fun play(seed: Long): Pair<Set<String>, Set<String>> {
        val game = build(seed)
        val lions = game.findPermanent("Savannah Lions")!!
        val courser = game.findPermanent("Centaur Courser")!!
        val casterBefore = game.state.getBattlefield().filter { game.state.projectedState.getController(it) == game.player1Id }.toSet()

        game.castSpell(1, "Whims of the Fates")
        game.resolveStack()

        // Starting with you: the caster separates first — Pile 1, then Pile 2 from the rest.
        val first = game.state.pendingDecision as SelectCardsDecision
        withClue("the caster separates first") { first.playerId shouldBe game.player1Id }
        withClue("every permanent the caster controls is up for the split, lands included") {
            first.options.toSet() shouldBe casterBefore
        }
        game.selectCards(listOf(lions))
        val second = game.state.pendingDecision as SelectCardsDecision
        withClue("the second split is still the caster's, over what Pile 1 left") {
            second.playerId shouldBe game.player1Id
            (lions in second.options) shouldBe false
        }
        game.selectCards(listOf(courser))

        // The random pick asks nobody: next is the opponent's own split.
        val third = game.state.pendingDecision as SelectCardsDecision
        withClue("the opponent separates their own permanents next") {
            third.playerId shouldBe game.player2Id
            third.options.size shouldBe 2
        }
        game.selectCards(third.options)
        // Pile 1 holds everything; the empty rest needs no second split.
        game.resolveStack()
        withClue("nothing is left to decide") { game.state.pendingDecision shouldBe null }

        fun graveyardNames(playerId: EntityId) =
            game.state.getGraveyard(playerId).mapNotNull {
                game.state.getEntity(it)?.get<CardComponent>()?.name
            }.filter { it != "Whims of the Fates" }.toSet()
        return graveyardNames(game.player1Id) to graveyardNames(game.player2Id)
    }

    init {
        cardRegistry.register(listOf(WhimsOfTheFates))

        test("each player sacrifices exactly one of their three piles, every pile equally possible") {
            val casterPiles = listOf(
                setOf("Savannah Lions"), setOf("Centaur Courser"), setOf("Grizzly Bears", "Mountain")
            )
            val casterOutcomes = mutableSetOf<Set<String>>()
            val opponentOutcomes = mutableSetOf<Set<String>>()
            for (seed in 1L..40L) {
                val (caster, opponent) = play(seed)
                withClue("seed $seed: the caster loses exactly one pile, got $caster") {
                    (caster in casterPiles) shouldBe true
                }
                withClue("seed $seed: the opponent loses all or nothing") {
                    (opponent.isEmpty() || opponent == setOf("Hill Giant", "Grizzly Bears")) shouldBe true
                }
                casterOutcomes += caster
                opponentOutcomes += opponent
            }
            withClue("each of the caster's three piles came up across seeds") {
                casterOutcomes shouldContainExactlyInAnyOrder casterPiles
            }
            withClue("the opponent's empty piles can be the random pick (two-thirds chance of keeping everything)") {
                opponentOutcomes shouldContainExactlyInAnyOrder listOf(emptySet(), setOf("Hill Giant", "Grizzly Bears"))
            }
        }
    }
}
