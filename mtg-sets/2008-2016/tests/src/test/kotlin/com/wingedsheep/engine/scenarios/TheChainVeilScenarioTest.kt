package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Chain Veil — end-step life loss unless you activated a loyalty ability this turn, and
 * "{4}, {T}: … activate one of its loyalty abilities once this turn as though none … have been
 * activated", proven with Ajani Goldmane's "+1: You gain 2 life."
 */
class TheChainVeilScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "The Chain Veil")
        .withCardOnBattlefield(1, "Ajani Goldmane")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.ajaniPlusOne() = run {
        val ajani = findPermanent("Ajani Goldmane")!!
        val plusOne = cardRegistry.getCard("Ajani Goldmane")!!.script.activatedAbilities
            .first { (it.cost as? AbilityCost.Loyalty)?.change == 1 }
        execute(ActivateAbility(player1Id, ajani, plusOne.id)).also { if (it.error == null) resolveStack() }
    }

    private fun TestGame.activateVeil() {
        val veil = findPermanent("The Chain Veil")!!
        val ability = cardRegistry.getCard("The Chain Veil")!!.script.activatedAbilities.single()
        execute(ActivateAbility(player1Id, veil, ability.id)).error shouldBe null
        resolveStack()
    }

    init {
        test("you lose 2 life at your end step if you didn't activate a loyalty ability") {
            val game = board()
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.getLifeTotal(1) shouldBe 18
        }

        test("activating a loyalty ability this turn stops the life loss") {
            val game = board()
            game.ajaniPlusOne().error shouldBe null
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.getLifeTotal(1) shouldBe 22
        }

        test("{4}, {T}: Ajani's loyalty ability can be activated a second time this turn") {
            val game = board()
            game.ajaniPlusOne().error shouldBe null
            withClue("once per turn without the Veil") { game.ajaniPlusOne().error shouldNotBe null }

            game.activateVeil()
            game.state.getEntity(game.findPermanent("The Chain Veil")!!)!!.has<TappedComponent>() shouldBe true

            game.ajaniPlusOne().error shouldBe null
            withClue("only one extra activation") { game.ajaniPlusOne().error shouldNotBe null }
            game.getLifeTotal(1) shouldBe 24
        }
    }
}
