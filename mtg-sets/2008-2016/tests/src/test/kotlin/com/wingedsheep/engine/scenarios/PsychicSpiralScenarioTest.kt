package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Psychic Spiral (RTR #47): "Shuffle all cards from your graveyard into your library. Target player
 * mills that many cards." The count must be taken before the shuffle empties the graveyard, and
 * Psychic Spiral itself (still on the stack) is neither shuffled in nor counted.
 */
class PsychicSpiralScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.castSpiralAt(targetPlayer: Int) {
        val cardId = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Psychic Spiral"
        }
        val targetId = if (targetPlayer == 1) player1Id else player2Id
        val cast = execute(CastSpell(player1Id, cardId, listOf(ChosenTarget.Player(targetId))))
        withClue("Casting Psychic Spiral should succeed: ${cast.error}") { cast.error shouldBe null }
        resolveStack()
    }

    init {
        test("shuffles your graveyard into your library and the target opponent mills that many") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Psychic Spiral")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Glory Seeker")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .apply { repeat(5) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpiralAt(2)

            withClue("Only Psychic Spiral is left in the caster's graveyard") {
                game.graveyardSize(1) shouldBe 1
                game.isInGraveyard(1, "Psychic Spiral") shouldBe true
            }
            withClue("The three graveyard cards joined the caster's library") { game.librarySize(1) shouldBe 4 }
            withClue("The opponent milled three cards") {
                game.graveyardSize(2) shouldBe 3
                game.librarySize(2) shouldBe 2
            }
        }

        test("targeting yourself mills the count taken before the shuffle") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Psychic Spiral")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Glory Seeker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpiralAt(1)

            withClue("Two cards were shuffled in, then two were milled back, then the spell itself") {
                game.librarySize(1) shouldBe 0
                game.graveyardSize(1) shouldBe 3
            }
        }
    }
}
