package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Part the Waterveil — "Take an extra turn after this one. Exile Part the Waterveil." with
 * awaken 6. Cast for its awaken cost it still grants the extra turn and still exiles itself.
 */
class PartTheWaterveilScenarioTest : ScenarioTestBase() {

    init {
        test("the caster takes the next turn too, and the spell is exiled rather than put in the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Part the Waterveil")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Part the Waterveil").error shouldBe null
            game.resolveStack()

            withClue("Part the Waterveil exiles itself as it resolves") {
                game.isInExile(1, "Part the Waterveil") shouldBe true
                game.isInGraveyard(1, "Part the Waterveil") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the extra turn belongs to the caster") {
                game.state.activePlayerId shouldBe game.player1Id
            }
        }

        test("cast for its awaken cost, it awakens a land, grants the extra turn and is still exiled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Part the Waterveil")
                .withLandsOnBattlefield(1, "Island", 9)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val land = game.findPermanent("Island")!!

            game.castSpellWithAwaken(1, "Part the Waterveil", listOf(land)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the awakened land is a 6/6 hasty creature") {
                projected.isCreature(land) shouldBe true
                projected.hasKeyword(land, Keyword.HASTE) shouldBe true
                projected.getPower(land) shouldBe 6
                projected.getToughness(land) shouldBe 6
            }
            withClue("the awakened cast still exiles the spell") {
                game.isInExile(1, "Part the Waterveil") shouldBe true
                game.isInGraveyard(1, "Part the Waterveil") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the extra turn belongs to the caster") {
                game.state.activePlayerId shouldBe game.player1Id
            }
        }
        test("awakened with its only target gone, it doesn't resolve: no extra turn, and it goes to the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Part the Waterveil")
                .withCardInHand(1, "Boomerang")
                .withLandsOnBattlefield(1, "Island", 11)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val land = game.findPermanent("Island")!!

            game.castSpellWithAwaken(1, "Part the Waterveil", listOf(land)).error shouldBe null
            game.castSpell(1, "Boomerang", land).error shouldBe null
            game.resolveStack()

            withClue("a spell that doesn't resolve never reaches its self-exile (ruling)") {
                game.isInGraveyard(1, "Part the Waterveil") shouldBe true
                game.isInExile(1, "Part the Waterveil") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("no extra turn: the opponent is next") {
                game.state.activePlayerId shouldBe game.player2Id
            }
        }
    }
}
