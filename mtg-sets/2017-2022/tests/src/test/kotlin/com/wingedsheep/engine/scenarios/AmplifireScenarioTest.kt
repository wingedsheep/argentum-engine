package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Amplifire (RNA #92) — {2}{R}{R} 1/1 Elemental. "At the beginning of your upkeep, reveal cards
 * from the top of your library until you reveal a creature card. Until your next turn, this
 * creature's base power becomes twice that card's power and its base toughness becomes twice that
 * card's toughness. Put the revealed cards on the bottom of your library in a random order."
 *
 * Covers: the base-P/T snapshot reads the revealed creature card's stats *before* the revealed cards
 * move to the bottom; the set outlives the end of the turn (lasts through the opponent's turn); and
 * the ruling that revealing no creature card leaves Amplifire a 0/0.
 */
class AmplifireScenarioTest : ScenarioTestBase() {

    init {
        context("Amplifire") {

            test("becomes twice the revealed creature's P/T until your next turn; revealed cards go to the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Amplifire")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id

                val amplifire = game.findPermanent("Amplifire")!!
                withClue("Craw Wurm (6/4) was revealed: Amplifire is 12/8") {
                    game.state.projectedState.getPower(amplifire) shouldBe 12
                    game.state.projectedState.getToughness(amplifire) shouldBe 8
                }
                withClue("Mountain and Craw Wurm went to the bottom; the draw took a Forest") {
                    val library = game.state.getLibrary(game.player1Id)
                        .map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                    library.size shouldBe 3
                    library.first() shouldBe "Forest"
                    library.takeLast(2) shouldContainExactlyInAnyOrder listOf("Mountain", "Craw Wurm")
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("the set lasts through the opponent's turn") {
                    game.state.projectedState.getPower(amplifire) shouldBe 12
                    game.state.projectedState.getToughness(amplifire) shouldBe 8
                }
            }

            test("revealing no creature card makes Amplifire a 0/0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Amplifire")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("a 0/0 Amplifire is put into its owner's graveyard") {
                    game.findPermanent("Amplifire") shouldBe null
                    game.isInGraveyard(1, "Amplifire") shouldBe true
                }
                withClue("the whole library went back (one card then drawn)") {
                    game.state.getLibrary(game.player1Id).size shouldBe 1
                    game.handSize(1) shouldNotBe 0
                }
            }
        }
    }
}
