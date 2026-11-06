package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Trespasser's Curse (AKH #112) — {1}{B} Enchantment — Aura Curse: "Enchant player / Whenever a
 * creature enchanted player controls enters, that player loses 1 life and you gain 1 life."
 *
 * The trigger's filter is scoped to the enchanted player, so only that player's creatures drain
 * them — the Curse controller's own creatures entering must not fire it.
 */
class TrespassersCurseScenarioTest : ScenarioTestBase() {

    /** Player 1 curses player 2 in their own main phase; both players hold a Grizzly Bears. */
    private fun cursedOpponent(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Trespasser's Curse")
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(2, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(2, "Forest", 2)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpellTargetingPlayer(1, "Trespasser's Curse", 2).error shouldBe null
        game.resolveStack()
        val curse = game.findPermanent("Trespasser's Curse")!!
        game.state.getEntity(curse)?.get<AttachedToComponent>()?.targetId shouldBe game.player2Id
        return game
    }

    init {
        context("Trespasser's Curse") {

            test("a creature entering under the enchanted player's control drains them for the Curse's controller") {
                val game = cursedOpponent()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                withClue("enchanted player loses 1") { game.getLifeTotal(2) shouldBe 19 }
                withClue("Curse's controller gains 1") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("a creature entering under the Curse controller's own control does not trigger it") {
                val game = cursedOpponent()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
