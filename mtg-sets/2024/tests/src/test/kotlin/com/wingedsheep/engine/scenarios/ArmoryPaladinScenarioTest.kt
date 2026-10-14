package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Armory Paladin (PIP #93) — {1}{R}{W} Creature — Human Knight 3/3.
 *
 *   Trample
 *   Whenever you cast an Aura or Equipment spell, exile the top card of your library. You may play
 *   that card until the end of your next turn.
 */
class ArmoryPaladinScenarioTest : ScenarioTestBase() {

    private fun board(spell: String, land: String, lands: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Armory Paladin")
        .withCardInHand(1, spell)
        .withLandsOnBattlefield(1, land, lands)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.exiledBears() = state.getExile(player1Id).filter { id ->
        state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
    }

    init {
        test("casting an Equipment spell exiles the top card, which can then be cast from exile") {
            // One Forest for Bonesplitter, two for the exiled Grizzly Bears.
            val game = board(spell = "Bonesplitter", land = "Forest", lands = 3)

            game.castSpell(1, "Bonesplitter").error shouldBe null
            game.resolveStack()

            game.findPermanent("Bonesplitter") shouldNotBe null
            withClue("the top card of the library was exiled") {
                game.librarySize(1) shouldBe 0
                game.isInExile(1, "Grizzly Bears") shouldBe true
            }
            val exiled = game.exiledBears()
            withClue("the exiled card carries a may-play permission") {
                exiled.size shouldBe 1
                exiled.forEach { id -> game.state.mayPlayPermissions.any { id in it.cardIds } shouldBe true }
            }

            game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("casting an Aura spell also triggers it") {
            val game = board(spell = "Holy Strength", land = "Plains", lands = 1)
            val paladin = game.findPermanent("Armory Paladin")!!

            game.castSpell(1, "Holy Strength", paladin).error shouldBe null
            game.resolveStack()

            game.librarySize(1) shouldBe 0
            game.isInExile(1, "Grizzly Bears") shouldBe true
        }

        test("casting a spell that is neither an Aura nor an Equipment doesn't trigger") {
            val game = board(spell = "Hill Giant", land = "Mountain", lands = 4)

            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.findPermanent("Hill Giant") shouldNotBe null
            game.librarySize(1) shouldBe 1
            game.isInExile(1, "Grizzly Bears") shouldBe false
        }
    }
}
