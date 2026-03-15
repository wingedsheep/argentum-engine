package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin Barrage.
 *
 * Card reference:
 * - Goblin Barrage ({3}{R}): Sorcery
 *   Kicker—Sacrifice an artifact or Goblin.
 *   Goblin Barrage deals 4 damage to target creature. If this spell was kicked,
 *   it also deals 4 damage to target player or planeswalker.
 */
class GoblinBarrageScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Barrage") {

            test("unkicked deals 4 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Barrage")
                    .withCardOnBattlefield(2, "Serra Angel") // 4/4
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Goblin Barrage", game.findPermanent("Serra Angel")!!)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Serra Angel is 4/4, takes 4 damage → dies
                withClue("Serra Angel (4/4) should die from 4 damage") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
            }

            test("unkicked deals 4 damage - creature with 5 toughness survives") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Barrage")
                    .withCardOnBattlefield(2, "Primordial Wurm") // 7/6
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Goblin Barrage", game.findPermanent("Primordial Wurm")!!)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Primordial Wurm (7/6) should survive 4 damage") {
                    game.isOnBattlefield("Primordial Wurm") shouldBe true
                }
            }

            test("kicked deals 4 to creature AND 4 to player by sacrificing a Goblin") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Barrage")
                    .withCardOnBattlefield(1, "Skirk Prospector") // Goblin to sacrifice
                    .withCardOnBattlefield(2, "Serra Angel") // 4/4
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val opponentId = game.player2Id
                val hand = game.state.getHand(playerId)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Goblin Barrage"
                }!!
                val angelId = game.findPermanent("Serra Angel")!!
                val goblinId = game.findPermanent("Skirk Prospector")!!

                val startingLife = game.getLifeTotal(2)

                val castResult = game.execute(
                    CastSpell(
                        playerId, cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(angelId),
                            ChosenTarget.Player(opponentId)
                        ),
                        wasKicked = true,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(goblinId))
                    )
                )
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Serra Angel (4/4) should die from 4 damage
                withClue("Serra Angel should die from 4 damage") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
                // Opponent should take 4 damage
                withClue("Opponent should take 4 damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 4
                }
                // Skirk Prospector was sacrificed
                withClue("Skirk Prospector should have been sacrificed") {
                    game.isOnBattlefield("Skirk Prospector") shouldBe false
                }
            }

            test("kicked works by sacrificing an artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Barrage")
                    .withCardOnBattlefield(1, "Aesthir Glider") // Artifact creature
                    .withCardOnBattlefield(2, "Serra Angel") // 4/4
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val opponentId = game.player2Id
                val hand = game.state.getHand(playerId)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Goblin Barrage"
                }!!
                val angelId = game.findPermanent("Serra Angel")!!
                val artifactId = game.findPermanent("Aesthir Glider")!!

                val startingLife = game.getLifeTotal(2)

                val castResult = game.execute(
                    CastSpell(
                        playerId, cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(angelId),
                            ChosenTarget.Player(opponentId)
                        ),
                        wasKicked = true,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifactId))
                    )
                )
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Serra Angel should die from 4 damage") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
                withClue("Opponent should take 4 damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 4
                }
                withClue("Aesthir Glider should have been sacrificed") {
                    game.isOnBattlefield("Aesthir Glider") shouldBe false
                }
            }
        }
    }
}
