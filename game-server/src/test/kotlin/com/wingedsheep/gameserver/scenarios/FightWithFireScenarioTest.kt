package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fight with Fire (kicker with alternate targeting).
 *
 * Card reference:
 * - Fight with Fire ({2}{R}): Sorcery
 *   Kicker {5}{R}
 *   Fight with Fire deals 5 damage to target creature.
 *   If this spell was kicked, it deals 10 damage divided as you choose
 *   among any number of targets instead.
 */
class FightWithFireScenarioTest : ScenarioTestBase() {

    init {
        context("Fight with Fire") {

            test("unkicked deals 5 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fight with Fire")
                    .withCardOnBattlefield(2, "Serra Angel") // 4/4
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angelId = game.findPermanent("Serra Angel")!!
                val castResult = game.castSpell(1, "Fight with Fire", angelId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Serra Angel (4/4) takes 5 damage → dies
                withClue("Serra Angel should die from 5 damage") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
            }

            test("kicked deals 10 damage to a single target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fight with Fire")
                    .withCardOnBattlefield(2, "Serra Angel") // 4/4
                    .withLandsOnBattlefield(1, "Mountain", 9) // {2}{R} + {5}{R} kicker = 9 mana
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Fight with Fire"
                }!!
                val angelId = game.findPermanent("Serra Angel")!!

                // Cast with kicker, single target — no distribution needed
                val castResult = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(angelId)),
                        wasKicked = true
                    )
                )
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Serra Angel should die from 10 damage") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
            }

            test("kicked divides 10 damage among multiple targets") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fight with Fire")
                    .withCardOnBattlefield(2, "Serra Angel")   // 4/4
                    .withCardOnBattlefield(2, "Hill Giant")     // 3/3
                    .withCardOnBattlefield(2, "Raging Goblin")  // 1/1
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Fight with Fire"
                }!!
                val angelId = game.findPermanent("Serra Angel")!!
                val giantId = game.findPermanent("Hill Giant")!!
                val goblinId = game.findPermanent("Raging Goblin")!!

                // Cast kicked: 5 to Serra Angel, 4 to Hill Giant, 1 to Raging Goblin
                val castResult = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(angelId),
                            ChosenTarget.Permanent(giantId),
                            ChosenTarget.Permanent(goblinId)
                        ),
                        wasKicked = true,
                        damageDistribution = mapOf(
                            angelId to 5,
                            giantId to 4,
                            goblinId to 1
                        )
                    )
                )
                withClue("Kicked cast with distribution should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Serra Angel (4/4) takes 5 → dies
                withClue("Serra Angel should die from 5 damage") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
                // Hill Giant (3/3) takes 4 → dies
                withClue("Hill Giant should die from 4 damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                // Raging Goblin (1/1) takes 1 → dies
                withClue("Raging Goblin should die from 1 damage") {
                    game.isOnBattlefield("Raging Goblin") shouldBe false
                }
            }

            test("kicked can target players") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fight with Fire")
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val opponentId = game.player2Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Fight with Fire"
                }!!

                // Cast kicked targeting opponent for 10 damage
                val castResult = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Player(opponentId)),
                        wasKicked = true
                    )
                )
                withClue("Kicked cast targeting player should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Opponent should have taken 10 damage (20 → 10)
                withClue("Opponent should be at 10 life") {
                    game.getLifeTotal(2) shouldBe 10
                }
            }
        }
    }
}
