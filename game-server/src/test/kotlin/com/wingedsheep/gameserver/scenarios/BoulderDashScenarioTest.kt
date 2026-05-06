package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Boulder Dash.
 *
 * Card reference:
 * - Boulder Dash ({1}{R}): Sorcery
 *   Boulder Dash deals 2 damage to any target and 1 damage to any other target.
 *
 * The second target must be different from the first ("any other target"), per
 * standard Magic targeting wording.
 */
class BoulderDashScenarioTest : ScenarioTestBase() {

    init {
        context("Boulder Dash targeting") {

            test("rejects casting with the same target chosen twice") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Boulder Dash")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 Human Soldier
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Boulder Dash"
                }!!
                val glorySeeker = game.findPermanent("Glory Seeker")!!

                val result = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(glorySeeker),
                            ChosenTarget.Permanent(glorySeeker)
                        )
                    )
                )

                withClue("Cast should fail when both targets are the same") {
                    result.error shouldNotBe null
                    result.error!!.shouldContain("different")
                }

                withClue("Glory Seeker should still be on the battlefield (cast fizzled)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }

            test("succeeds with two distinct targets and deals 2 + 1 damage") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Boulder Dash")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2; takes 2 damage and dies
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Boulder Dash"
                }!!
                val glorySeeker = game.findPermanent("Glory Seeker")!!

                val result = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(glorySeeker),
                            ChosenTarget.Player(game.player2Id)
                        )
                    )
                )

                withClue("Cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Glory Seeker should die from 2 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("Opponent should have taken 1 damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }
    }
}
