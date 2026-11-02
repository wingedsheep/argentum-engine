package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * The proliferate decision's `recommendedSelection` — the one-click "Recommended" pick the client
 * offers next to "All". It is advice, not a rule (CR 701.34a lets the player choose any subset), so
 * these tests pin the rule of thumb itself:
 *
 * - your own permanents and yourself are recommended unless they carry a counter that hurts their
 *   holder (-1/-1, stun, poison);
 * - an opponent's permanents and the opponent are recommended only when they carry such a counter
 *   and none that helps them (+1/+1, loyalty, keyword counters);
 * - a counter kind with no clear polarity (charge) is fine on your side and no reason on theirs;
 * - submitting the recommendation is a legal selection that proliferates exactly those.
 */
class ProliferateRecommendationTest : ScenarioTestBase() {

    private val spark = card("Test Proliferate Spark") {
        manaCost = "{1}"
        typeLine = "Instant"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private val battery = card("Test Charge Battery") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    private val opposingBattery = card("Test Opposing Battery") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int = 1) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun proliferateDecision(game: TestGame): SelectCardsDecision {
        game.castSpell(1, "Test Proliferate Spark").error shouldBe null
        game.resolveStack()
        return game.getPendingDecision() as SelectCardsDecision
    }

    private fun baseScenario() = scenario()
        .withPlayers("Player1", "Player2")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInHand(1, "Test Proliferate Spark")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        listOf(spark, battery, opposingBattery).forEach(cardRegistry::register)

        test("recommends growing your side and shrinking theirs") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Test Charge Battery")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardOnBattlefield(2, "Test Opposing Battery")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val myBattery = game.findPermanent("Test Charge Battery")!!
            val courser = game.findPermanent("Centaur Courser")!!
            val elves = game.findPermanent("Llanowar Elves")!!
            val theirBattery = game.findPermanent("Test Opposing Battery")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, giant, CounterType.MINUS_ONE_MINUS_ONE)
            seed(game, myBattery, CounterType.CHARGE)
            seed(game, courser, CounterType.MINUS_ONE_MINUS_ONE)
            seed(game, elves, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, theirBattery, CounterType.CHARGE)
            seed(game, game.player2Id, CounterType.POISON)
            seed(game, game.player1Id, CounterType.EXPERIENCE)

            val decision = proliferateDecision(game)
            decision.options shouldContainExactlyInAnyOrder listOf(
                bears, giant, myBattery, courser, elves, theirBattery, game.player1Id, game.player2Id,
            )
            withClue("your +1/+1 creature, charged artifact and experience; their -1/-1 creature and poison") {
                decision.recommendedSelection!! shouldContainExactlyInAnyOrder listOf(
                    bears, myBattery, game.player1Id, courser, game.player2Id,
                )
            }

            game.selectCards(decision.recommendedSelection!!).error shouldBe null
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            counters(game, courser, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
            counters(game, game.player2Id, CounterType.POISON) shouldBe 2
            withClue("the passed-over recipients keep what they had") {
                counters(game, giant, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
                counters(game, elves, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                counters(game, theirBattery, CounterType.CHARGE) shouldBe 1
            }
        }

        test("a poisoned you, a stunned creature of yours and a mixed-counter opposing creature are left out") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, bears, CounterType.STUN)
            seed(game, courser, CounterType.MINUS_ONE_MINUS_ONE)
            seed(game, courser, CounterType.FLYING)
            seed(game, game.player1Id, CounterType.POISON)

            val decision = proliferateDecision(game)
            decision.options shouldContainExactlyInAnyOrder listOf(bears, courser, game.player1Id)
            decision.recommendedSelection!! shouldBe emptyList()
        }
    }
}
