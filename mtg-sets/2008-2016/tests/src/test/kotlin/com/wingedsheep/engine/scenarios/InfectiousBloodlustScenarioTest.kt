package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Infectious Bloodlust (ORI #152) — {1}{R} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +2/+1, has haste, and attacks each combat if able.
 *   When enchanted creature dies, you may search your library for a card named Infectious
 *   Bloodlust, reveal it, put it into your hand, then shuffle.
 *
 * Pins the static grant and the dies → search chain: the search offers only cards named
 * Infectious Bloodlust, "you may" can be declined, and "you" is the Aura's controller even
 * when the Aura enchanted an opponent's creature.
 */
class InfectiousBloodlustScenarioTest : ScenarioTestBase() {

    init {
        /** Player 1 bolts the creature wearing Player 1's Bloodlust; the creature is [hostOwner]'s. */
        fun boltTheHost(hostOwner: Int): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(hostOwner, "Grizzly Bears")
                .withCardAttachedTo(1, "Infectious Bloodlust", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Infectious Bloodlust")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.castSpell(1, "Lightning Bolt", bears)
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("the 2/2 + 2/1 = 4/3 host died to 3 damage, the Aura went to the graveyard") {
                game.isInGraveyard(hostOwner, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Infectious Bloodlust") shouldBe true
            }
            return game
        }

        test("enchanted creature gets +2/+1 and haste") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Infectious Bloodlust", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
        }

        test("when the enchanted creature dies, may search for another Infectious Bloodlust") {
            val game = boltTheHost(hostOwner = 1)
            val copy = game.findCardsInLibrary(1, "Infectious Bloodlust").single()

            game.answerYesNo(true)
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only cards named Infectious Bloodlust are offered") {
                decision.options shouldBe listOf(copy)
            }
            game.selectCards(listOf(copy))
            game.resolveStack()

            game.isInHand(1, "Infectious Bloodlust") shouldBe true
            game.findCardsInLibrary(1, "Infectious Bloodlust").size shouldBe 0
            game.findCardsInLibrary(1, "Hill Giant").size shouldBe 1
        }

        test("declining the search leaves the copy in the library") {
            val game = boltTheHost(hostOwner = 1)

            game.answerYesNo(false)
            game.resolveStack()

            game.isInHand(1, "Infectious Bloodlust") shouldBe false
            game.findCardsInLibrary(1, "Infectious Bloodlust").size shouldBe 1
        }

        test("on an opponent's creature, the Aura's controller searches their own library") {
            val game = boltTheHost(hostOwner = 2)

            withClue("the trigger belongs to the Aura's controller, not the creature's") {
                game.getPendingDecision()?.playerId shouldBe game.player1Id
            }
            val copy = game.findCardsInLibrary(1, "Infectious Bloodlust").single()
            game.answerYesNo(true)
            game.selectCards(listOf(copy))
            game.resolveStack()

            game.isInHand(1, "Infectious Bloodlust") shouldBe true
        }
    }
}
