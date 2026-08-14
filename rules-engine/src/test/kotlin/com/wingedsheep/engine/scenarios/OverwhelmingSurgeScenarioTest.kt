package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Overwhelming Surge. */
class OverwhelmingSurgeScenarioTest : ScenarioTestBase() {

    init {
        context("Overwhelming Surge") {
            test("mode 0 deals 3 damage to a creature, killing a 3-toughness creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Overwhelming Surge")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 — dies to 3 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Hill Giant")!!
                val cast = game.castSpellWithMode(1, "Overwhelming Surge", modeIndex = 0, targetId = victim)
                withClue("Cast (damage mode) should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant should be dead") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }

            test("mode 2 deals 3 damage to a creature and destroys a noncreature artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Overwhelming Surge")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Mind Stone") // Artifact (noncreature artifact)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Hill Giant")!!
                val artifact = game.findPermanent("Mind Stone")!!
                val modeTargets = listOf(
                    ChosenTarget.Permanent(creature),
                    ChosenTarget.Permanent(artifact),
                )
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Overwhelming Surge").first(),
                        modeTargets, // flat union of mode targets
                        chosenModes = listOf(2),
                        modeTargetsOrdered = listOf(modeTargets)
                    )
                )
                withClue("Cast (both modes) should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant should be dead and Mind Stone destroyed") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Mind Stone") shouldBe false
                }
            }
        }
    }
}
