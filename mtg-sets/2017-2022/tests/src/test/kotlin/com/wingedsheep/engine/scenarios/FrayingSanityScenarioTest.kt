package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fraying Sanity (HOU #35) — {2}{U} Enchantment — Aura Curse. Enchant player. At the beginning of
 * each end step, enchanted player mills X cards, where X is the number of cards put into their
 * graveyard from anywhere this turn.
 *
 * The Aura is attached directly to the cursed player (as in Curse of Hospitality's test): the
 * builder's `withCardAttachedTo` only takes permanent hosts.
 */
class FrayingSanityScenarioTest : ScenarioTestBase() {

    init {
        fun cursed(cursedPlayer: Int): TestGame {
            var b = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Fraying Sanity")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Mind Rot")
                .withCardInHand(2, "Forest")
                .withCardInHand(2, "Forest")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(10) { b = b.withCardInLibrary(1, "Island") }
            repeat(10) { b = b.withCardInLibrary(2, "Island") }
            val game = b.build()
            val curse = game.findPermanent("Fraying Sanity")!!
            val victim = if (cursedPlayer == 1) game.player1Id else game.player2Id
            game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(victim)) }
            return game
        }

        context("Fraying Sanity") {
            test("the enchanted player mills one card for each card put into their graveyard this turn") {
                val game = cursed(cursedPlayer = 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
                game.resolveStack()
                withClue("Grizzly Bears died and two cards were discarded") {
                    game.graveyardSize(2) shouldBe 3
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("three cards reached the cursed player's graveyard, so they mill three") {
                    game.librarySize(2) shouldBe 7
                    game.graveyardSize(2) shouldBe 6
                }
                withClue("the curse's controller is unaffected; Bolt and Mind Rot went to their graveyard") {
                    game.librarySize(1) shouldBe 10
                }
            }

            test("cards put into another player's graveyard don't count") {
                val game = cursed(cursedPlayer = 2)
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                withClue("the Bolt went to its owner's graveyard, not the cursed player's") {
                    game.graveyardSize(1) shouldBe 1
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("nothing reached the cursed player's graveyard, so they mill nothing") {
                    game.librarySize(2) shouldBe 10
                    game.graveyardSize(2) shouldBe 0
                }
            }
        }
    }
}
