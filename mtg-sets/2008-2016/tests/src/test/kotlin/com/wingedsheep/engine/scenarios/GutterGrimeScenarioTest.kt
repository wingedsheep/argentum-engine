package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Gutter Grime (ISD #186).
 *
 * "Whenever a nontoken creature you control dies, put a slime counter on this enchantment, then
 * create a green Ooze creature token with 'This token's power and toughness are each equal to the
 * number of slime counters on Gutter Grime.'"
 *
 * Rulings: the Oozes' P/T update as Gutter Grime gains counters; with two Gutter Grimes each Ooze
 * counts only the one that created it; once that Gutter Grime leaves the battlefield its Oozes are
 * 0/0 and die to state-based actions.
 */
class GutterGrimeScenarioTest : ScenarioTestBase() {

    private fun board(grimes: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .also { b -> repeat(grimes) { b.withCardOnBattlefield(1, "Gutter Grime") } }
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardsInHand(1, "Lightning Bolt", 3)
        .withCardInHand(1, "Disenchant")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.bolt(name: String) {
        castSpell(1, "Lightning Bolt", findPermanent(name)!!).error shouldBe null
        resolveStack()
    }

    private fun TestGame.oozes() = findPermanents("Ooze Token")

    private fun TestGame.pt(id: com.wingedsheep.sdk.model.EntityId) =
        state.projectedState.getPower(id) to state.projectedState.getToughness(id)

    init {
        test("each nontoken death adds a slime counter and an Ooze; every Ooze grows with the counters") {
            val game = board()
            game.bolt("Grizzly Bears")
            game.oozes() shouldHaveSize 1
            game.pt(game.oozes().single()) shouldBe (1 to 1)

            game.bolt("Hill Giant")
            game.oozes() shouldHaveSize 2
            withClue("both Oozes read the two slime counters now on Gutter Grime") {
                game.oozes().forEach { game.pt(it) shouldBe (2 to 2) }
            }
        }

        test("a token creature dying doesn't trigger it") {
            val game = board()
            game.bolt("Grizzly Bears")
            game.bolt("Ooze Token")
            game.oozes().shouldBeEmpty()
            val grime = game.findPermanent("Gutter Grime")!!
            game.state.getEntity(grime)!!.get<CountersComponent>()!!.getCount(CounterType.SLIME) shouldBe 1
        }

        test("when Gutter Grime leaves the battlefield its Oozes become 0/0 and die") {
            val game = board()
            game.bolt("Grizzly Bears")
            game.oozes() shouldHaveSize 1
            game.castSpell(1, "Disenchant", game.findPermanent("Gutter Grime")!!).error shouldBe null
            game.resolveStack()
            game.oozes().shouldBeEmpty()
        }

        test("with two Gutter Grimes each Ooze counts only the one that created it") {
            val game = board(grimes = 2)
            game.bolt("Grizzly Bears")
            withClue("both Grimes trigger: one counter on each, one Ooze from each") {
                game.oozes() shouldHaveSize 2
                game.oozes().forEach { game.pt(it) shouldBe (1 to 1) }
            }
            game.castSpell(1, "Disenchant", game.findPermanents("Gutter Grime").first()).error shouldBe null
            game.resolveStack()
            withClue("only the destroyed Grime's Ooze dies; the other still reads its own Grime") {
                game.oozes() shouldHaveSize 1
                game.pt(game.oozes().single()) shouldBe (1 to 1)
            }
        }
    }
}
