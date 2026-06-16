package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pull from the Grave {2}{B} Sorcery — "Return up to two target creature cards from your
 * graveyard to your hand. You gain 2 life."
 *
 * Covers returning two creature cards, the optional "up to" (zero targets still gains life), and
 * the 2-life gain.
 */
class PullFromTheGraveScenarioTest : ScenarioTestBase() {

    init {
        context("Pull from the Grave — reanimate up to two creatures to hand, gain 2 life") {

            test("returns two targeted creature cards to hand and gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pull from the Grave")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val giant = game.findCardsInGraveyard(1, "Centaur Courser").single()
                val spell = game.findCardsInHand(1, "Pull from the Grave").single()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD),
                        ),
                    ),
                ).error shouldBe null
                game.resolveStack()

                withClue("Both creatures returned to hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Centaur Courser") shouldBe true
                }
                withClue("Neither remains in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Centaur Courser") shouldBe false
                }
                withClue("Controller gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("with zero targets chosen it still gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pull from the Grave")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Pull from the Grave").single()

                game.execute(CastSpell(game.player1Id, spell, emptyList())).error shouldBe null
                game.resolveStack()

                withClue("Creature stays in the graveyard when not targeted") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Controller still gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }
    }
}
