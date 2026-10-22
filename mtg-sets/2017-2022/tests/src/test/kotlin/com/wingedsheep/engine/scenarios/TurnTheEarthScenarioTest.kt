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
 * Scenario test for Turn the Earth (MID #205) — {G} Instant.
 *
 *   Choose up to three target cards in graveyards. The owners of those cards shuffle them into
 *   their libraries. You gain 2 life.
 *   Flashback {1}{G}
 *
 * Pins owner routing across graveyards (each card lands in its owner's library) and the
 * zero-target case (still gains 2 life).
 */
class TurnTheEarthScenarioTest : ScenarioTestBase() {

    init {
        context("Turn the Earth") {

            test("shuffles chosen cards from both graveyards into their owners' libraries and gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Turn the Earth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Divination")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Turn the Earth").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
                val giant = game.findCardsInGraveyard(2, "Hill Giant").single()

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(bolt, game.player2Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(giant, game.player2Id, Zone.GRAVEYARD),
                        )
                    )
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("each chosen card goes to its owner's library") {
                    game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInLibrary(2, "Lightning Bolt").size shouldBe 1
                    game.findCardsInLibrary(2, "Hill Giant").size shouldBe 1
                }
                withClue("the unchosen card stays in the graveyard") {
                    game.isInGraveyard(1, "Divination") shouldBe true
                }
                game.getLifeTotal(1) shouldBe 22
            }

            test("with no targets chosen it still gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Turn the Earth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Turn the Earth").single()
                val cast = game.execute(CastSpell(game.player1Id, spell, emptyList()))
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.getLifeTotal(1) shouldBe 22
            }
        }
    }
}
