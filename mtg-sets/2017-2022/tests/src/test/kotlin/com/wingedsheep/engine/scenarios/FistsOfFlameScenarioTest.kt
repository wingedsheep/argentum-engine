package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fists of Flame {1}{R} Instant — "Draw a card. Until end of turn, target creature gains trample
 * and gets +1/+0 for each card you've drawn this turn."
 *
 * The load-bearing part is ordering: the bonus counts the card Fists itself just drew (so it is
 * never less than +1), and it is locked in at resolution — later draws don't grow it.
 */
class FistsOfFlameScenarioTest : ScenarioTestBase() {

    init {
        context("Fists of Flame — +1/+0 per card drawn this turn") {

            test("with no earlier draws, the bonus counts Fists' own draw: +1/+0 and trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fists of Flame")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Centaur Courser") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Fists of Flame", courser).error shouldBe null
                game.resolveStack()

                withClue("Fists drew a card") {
                    game.isInHand(1, "Hill Giant") shouldBe true
                }
                val projected = game.state.projectedState
                withClue("One card drawn this turn (Fists' own) → +1/+0") {
                    projected.getPower(courser) shouldBe 4
                    projected.getToughness(courser) shouldBe 3
                }
                withClue("The target gains trample") {
                    projected.hasKeyword(courser, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("earlier draws count, and the bonus is locked in at resolution") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Divination")      // {2}{U} sorcery — draw two
                    .withCardInHand(1, "Fists of Flame")
                    .withCardInHand(1, "Divination")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Centaur Courser") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Fists of Flame", courser).error shouldBe null
                game.resolveStack()

                withClue("Two from Divination + one from Fists → +3/+0") {
                    game.state.projectedState.getPower(courser) shouldBe 6
                }

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                withClue("Drawing more later in the turn doesn't grow the locked-in bonus") {
                    game.state.projectedState.getPower(courser) shouldBe 6
                }
            }
        }
    }
}
