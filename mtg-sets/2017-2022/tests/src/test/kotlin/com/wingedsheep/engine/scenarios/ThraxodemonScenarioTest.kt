package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thraxodemon (BRO #115) — {3}, {T}, Sacrifice another creature or artifact: Draw a card.
 */
class ThraxodemonScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Thraxodemon")!!.activatedAbilities.first().id

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Thraxodemon")
        .withCardOnBattlefield(1, "Ornithopter")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("sacrificing another artifact taps the demon and draws a card") {
            val game = game()
            val demon = game.findPermanent("Thraxodemon")!!
            val fodder = game.findPermanent("Ornithopter")!!
            val handBefore = game.handSize(1)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = demon,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.state.getBattlefield().contains(demon) shouldBe true
            game.handSize(1) shouldBe handBefore + 1
        }

        test("cannot sacrifice itself to pay the cost") {
            val game = game()
            val demon = game.findPermanent("Thraxodemon")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = demon,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(demon)),
                )
            ).error shouldNotBe null
            game.isOnBattlefield("Thraxodemon") shouldBe true
        }
    }
}
