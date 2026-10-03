package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.GenkuFutureShaper
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Genku, Future Shaper — "Whenever another nontoken permanent you control leaves the battlefield,
 * choose one that hasn't been chosen this turn. Create a creature token with those
 * characteristics. • 2/2 white Fox with vigilance. • 1/2 blue Moonfolk with flying. • 1/1 black
 * Rat with lifelink. {3}{W}{U}: Put a +1/+1 counter on each creature you control."
 */
class GenkuFutureShaperScenarioTest : ScenarioTestBase() {

    private val foxMode = "2/2 white Fox with vigilance"
    private val moonfolkMode = "1/2 blue Moonfolk with flying"
    private val ratMode = "1/1 black Rat with lifelink"

    private fun TestGame.resolveToModeChoice(): ChooseOptionDecision {
        var guard = 0
        while (getPendingDecision() !is ChooseOptionDecision && guard++ < 20) {
            resolveStack()
        }
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun TestGame.permanentsNamed(name: String): List<EntityId> =
        state.getBattlefield().filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("each nontoken permanent leaving triggers with a fresh mode; tokens leaving don't trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Genku, Future Shaper")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInHand(1, "Pyroclasm")
                .withCardInHand(1, "Pyroclasm")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Pyroclasm kills both Bears (Genku, a 2/5, survives) — two triggers.
            game.castSpell(1, "Pyroclasm").error shouldBe null

            val first = game.resolveToModeChoice()
            first.options.size shouldBe 3
            game.chooseMode(first, foxMode)

            val second = game.resolveToModeChoice()
            withClue("the Fox mode was already chosen this turn") {
                second.options.size shouldBe 2
                second.options shouldNotContain foxMode
                second.options shouldContain moonfolkMode
                second.options shouldContain ratMode
            }
            game.chooseMode(second, ratMode)
            game.resolveStack()

            game.permanentsNamed("Grizzly Bears").size shouldBe 0
            game.findPermanent("Genku, Future Shaper").shouldNotBeNull()

            val fox = game.permanentsNamed("Fox Token").ifEmpty { game.permanentsNamed("Fox") }
            withClue("one 2/2 white Fox with vigilance") {
                fox.size shouldBe 1
                val projected = game.state.projectedState
                projected.getPower(fox.single()) shouldBe 2
                projected.getToughness(fox.single()) shouldBe 2
                projected.hasKeyword(fox.single(), Keyword.VIGILANCE) shouldBe true
            }
            val rat = game.permanentsNamed("Rat Token").ifEmpty { game.permanentsNamed("Rat") }
            withClue("one 1/1 Rat with lifelink") {
                rat.size shouldBe 1
                game.state.projectedState.hasKeyword(rat.single(), Keyword.LIFELINK) shouldBe true
            }

            // Second Pyroclasm kills only the two tokens: tokens leaving don't trigger Genku.
            game.castSpell(1, "Pyroclasm").error shouldBe null
            game.resolveStack()
            withClue("no trigger for tokens leaving the battlefield") {
                (game.getPendingDecision() is ChooseOptionDecision) shouldBe false
                game.state.stack.size shouldBe 0
            }
            game.findPermanent("Genku, Future Shaper").shouldNotBeNull()
        }

        test("activated ability puts a +1/+1 counter on each creature you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Genku, Future Shaper")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val genku = game.findPermanent("Genku, Future Shaper")!!
            val bears = game.permanentsNamed("Grizzly Bears")
            val myBears = bears.single { game.state.projectedState.getController(it) == game.player1Id }
            val theirBears = bears.single { it != myBears }

            game.execute(
                ActivateAbility(game.player1Id, genku, GenkuFutureShaper.activatedAbilities.first().id)
            ).error shouldBe null
            game.resolveStack()

            game.plusOneCounters(genku) shouldBe 1
            game.plusOneCounters(myBears) shouldBe 1
            game.plusOneCounters(theirBears) shouldBe 0
        }
    }
}
