package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zada's Commando — {1}{R} 2/1 Goblin Archer Ally, first strike.
 * "Cohort — {T}, Tap an untapped Ally you control: This creature deals 1 damage to target opponent
 * or planeswalker."
 */
class ZadasCommandoScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Zada's Commando")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Zada's Commando")
            .withCardOnBattlefield(1, "Kor Sky Climber")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("cohort taps both Allies and deals 1 damage to the opponent") {
            val game = board()
            val commando = game.findPermanent("Zada's Commando")!!
            val ally = game.findPermanent("Kor Sky Climber")!!

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, commando, cohortId, listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 20
            game.state.getEntity(commando)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(ally)?.has<TappedComponent>() shouldBe true
        }

        test("you can't target yourself") {
            val game = board()
            val commando = game.findPermanent("Zada's Commando")!!
            val ally = game.findPermanent("Kor Sky Climber")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, commando, cohortId, listOf(ChosenTarget.Player(game.player1Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            ).error shouldNotBe null
            game.getLifeTotal(1) shouldBe 20
        }

        test("a non-Ally creature can't pay the cohort cost") {
            val game = board()
            val commando = game.findPermanent("Zada's Commando")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, commando, cohortId, listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
