package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Thalisse, Reverent Medium (CMR #291).
 *
 * "At the beginning of each end step, create X 1/1 white Spirit creature tokens with flying, where X
 *  is the number of tokens you created this turn."
 *
 * Counts tokens you created this turn, gone or not; a creature-spell copy that resolved into a token
 * wasn't created and doesn't count (2020-11-10 ruling); and it triggers on every player's end step.
 */
class ThalisseReverentMediumScenarioTest : ScenarioTestBase() {

    private fun TestGame.spirits(): Int = findPermanents("Spirit Token").size

    init {
        context("Thalisse, Reverent Medium") {

            test("your end step makes one Spirit per token you created this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thalisse, Reverent Medium")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                game.findPermanents("Soldier Token").size shouldBe 2

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Two Soldiers created → two flying Spirits") {
                    game.spirits() shouldBe 2
                    game.findPermanents("Spirit Token").all {
                        game.state.getEntity(it)?.has<TokenComponent>() == true &&
                            game.state.projectedState.hasKeyword(it, Keyword.FLYING)
                    } shouldBe true
                }
            }

            test("no tokens created — the end-step trigger makes nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thalisse, Reverent Medium")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.spirits() shouldBe 0
            }

            test("tokens you create on an opponent's turn count at that turn's end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thalisse, Reverent Medium")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                game.findPermanents("Soldier Token").size shouldBe 2

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.spirits() shouldBe 2
            }

            test("a creature-spell copy resolving into a token wasn't created and doesn't count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thalisse, Reverent Medium")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Double Major")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                val copy = game.castSpellTargetingStackSpell(1, "Double Major", "Grizzly Bears")
                withClue("Double Major: ${copy.error}") { copy.error shouldBe null }
                game.resolveStack()

                val bears = game.findPermanents("Grizzly Bears")
                withClue("The original and its token copy are both on the battlefield") {
                    bears.size shouldBe 2
                    bears.count { game.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 1
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.spirits() shouldBe 0
            }
        }
    }
}
