package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pip-Boy 3000 (PIP #140) — {1} Artifact — Equipment
 *
 *   Whenever equipped creature attacks, choose one —
 *   • Sort Inventory — Draw a card, then discard a card.
 *   • Pick a Perk — Put a +1/+1 counter on that creature.
 *   • Check Map — Untap up to two target lands.
 *   Equip {2}
 *
 * "That creature" is the attacker the trigger fired for (TriggeringEntity under an attached
 * trigger), read from inside a no-target mode — the counter must land on it, not on the Equipment.
 */
class PipBoy3000ScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseMode(index: Int) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    init {
        context("Pip-Boy 3000") {

            test("Pick a Perk puts a +1/+1 counter on the attacking equipped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Pip-Boy 3000", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val pipBoy = game.findPermanent("Pip-Boy 3000")!!

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.chooseMode(1)
                game.resolveStack()

                withClue("one +1/+1 counter on the attacking equipped creature") {
                    plusOnes(game, bears) shouldBe 1
                }
                withClue("none on the Equipment") {
                    plusOnes(game, pipBoy) shouldBe 0
                }
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
            }

            test("Check Map untaps two target lands, leaving the third tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Pip-Boy 3000", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val forests = game.findPermanents("Forest")
                forests.size shouldBe 3
                forests.all { isTapped(game, it) } shouldBe true

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.chooseMode(2)
                game.selectTargets(forests.take(2)).error shouldBe null
                game.resolveStack()

                withClue("the two targeted lands untap") {
                    isTapped(game, forests[0]) shouldBe false
                    isTapped(game, forests[1]) shouldBe false
                }
                withClue("the untargeted land stays tapped") {
                    isTapped(game, forests[2]) shouldBe true
                }
            }
        }
    }
}
