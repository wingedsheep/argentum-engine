package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class DovinGrandArbiterScenarioTest : ScenarioTestBase() {
    private val name = "Dovin, Grand Arbiter"

    private fun loyalty(game: TestGame): Int =
        game.state.getEntity(game.findPermanent(name)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    private fun activate(game: TestGame, index: Int, loyalty: Int) {
        val source = game.findPermanent(name)!!
        game.state = game.state.updateEntity(source) {
            it.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty))
        }
        game.execute(
            ActivateAbility(game.player1Id, source, cardRegistry.getCard(name)!!.script.activatedAbilities[index].id)
        ).error shouldBe null
        game.resolveStack()
    }

    init {
        test("+1 adds one loyalty counter per creature that deals combat damage to a player this turn") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            activate(game, 0, loyalty = 3)
            loyalty(game) shouldBe 4

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            withClue("5 damage from two creatures is two triggers, one counter each") {
                game.getLifeTotal(2) shouldBe 15
                loyalty(game) shouldBe 6
            }
        }

        test("−1 creates a flying Thopter and gains 1 life") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            activate(game, 1, loyalty = 3)
            val thopters = game.state.getBattlefield(game.player1Id).filter {
                game.state.projectedState.hasSubtype(it, "Thopter")
            }
            thopters.size shouldBe 1
            val thopter = thopters.single()
            game.state.projectedState.hasType(thopter, "ARTIFACT") shouldBe true
            game.state.projectedState.hasKeyword(thopter, Keyword.FLYING) shouldBe true
            game.state.projectedState.getPower(thopter) shouldBe 1
            game.getLifeTotal(1) shouldBe 21
            loyalty(game) shouldBe 2
        }

        test("−7 puts three of the top ten into hand and the rest on the bottom") {
            val builder = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardInLibrary(2, "Island")
            repeat(12) { builder.withCardInLibrary(1, "Island") }
            val game = builder.withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            val libraryBefore = game.state.getLibrary(game.player1Id)
            val untouched = libraryBefore.drop(10)
            activate(game, 2, loyalty = 7)

            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.options.size shouldBe 10
            val picks = decision.options.take(3)
            game.selectCards(picks).error shouldBe null
            game.resolveStack()

            val hand = game.state.getHand(game.player1Id)
            hand.size shouldBe 3
            hand.toSet() shouldBe picks.toSet()
            val library = game.state.getLibrary(game.player1Id)
            library.size shouldBe 9
            withClue("the two cards below the top ten are now on top; the seven looked-at cards are underneath") {
                library.take(2) shouldBe untouched
                library.drop(2).toSet() shouldBe (decision.options - picks.toSet()).toSet()
            }
        }
    }
}
