package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Elven Bow (KHM #166, reprinted J22) — {G} Artifact — Equipment.
 *
 *   When this Equipment enters, you may pay {2}. If you do, create a 1/1 green Elf Warrior
 *   creature token, then attach this Equipment to it.
 *   Equipped creature gets +1/+2 and has reach.
 *   Equip {3}
 *
 * Pins the gated chain: the token minted behind the {2} payment is the one the Bow attaches to.
 */
class ElvenBowScenarioTest : ScenarioTestBase() {

    private fun settle(game: TestGame, pay: Boolean) {
        var guard = 0
        while (guard++ < 30) {
            when (val decision = game.getPendingDecision()) {
                is YesNoDecision -> game.answerYesNo(pay)
                is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else return
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun board(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Elven Bow")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Elven Bow") {

            test("paying {2} creates an Elf Warrior and attaches the Bow to it") {
                val game = board()

                game.castSpell(1, "Elven Bow").error shouldBe null
                settle(game, pay = true)

                val bow = game.findPermanent("Elven Bow")!!
                val elf = game.findPermanent("Elf Warrior Token")
                elf shouldNotBe null
                game.state.getEntity(bow)?.get<AttachedToComponent>()?.targetId shouldBe elf

                val projected = game.state.projectedState
                projected.getPower(elf!!) shouldBe 2
                projected.getToughness(elf) shouldBe 3
                projected.hasKeyword(elf, Keyword.REACH) shouldBe true
            }

            test("declining leaves the Bow unattached and makes no token") {
                val game = board()

                game.castSpell(1, "Elven Bow").error shouldBe null
                settle(game, pay = false)

                val bow = game.findPermanent("Elven Bow")!!
                game.findPermanent("Elf Warrior Token") shouldBe null
                game.state.getEntity(bow)?.get<AttachedToComponent>() shouldBe null
            }
        }
    }
}
