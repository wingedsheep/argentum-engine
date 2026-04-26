package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cinder Strike.
 *
 * Card reference:
 * - Cinder Strike ({R}): Sorcery
 *   "As an additional cost to cast this spell, you may blight 1.
 *    Cinder Strike deals 2 damage to target creature.
 *    It deals 4 damage to that creature instead if this spell's additional cost was paid."
 */
class CinderStrikeScenarioTest : ScenarioTestBase() {

    init {
        context("Cinder Strike - blight path (4 damage)") {

            test("deals 4 damage to target creature when blight 1 is paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cinder Strike")
                    .withCardOnBattlefield(1, "Grizzly Bears") // creature to blight (2/2)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 target
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyBears = game.findPermanent("Grizzly Bears")!!
                val hillGiant = game.findPermanent("Hill Giant")!!

                val cinderStrike = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Cinder Strike"
                }

                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        cinderStrike,
                        listOf(ChosenTarget.Permanent(hillGiant)),
                        additionalCostPayment = AdditionalCostPayment(
                            blightTargets = listOf(grizzlyBears)
                        )
                    )
                )
                withClue("Cinder Strike (blight path) should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Grizzly Bears should have one -1/-1 counter from blight payment
                val counters = game.state.getEntity(grizzlyBears)?.get<CountersComponent>()
                withClue("Grizzly Bears should have 1 -1/-1 counter") {
                    counters.shouldNotBeNull()
                    counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
                }

                game.resolveStack()

                // Hill Giant (3/3) takes 4 damage → dies
                withClue("Hill Giant should be in graveyard after taking 4 damage") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("Hill Giant should not be on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }
        }

        context("Cinder Strike - skip blight (2 damage)") {

            test("deals 2 damage to target creature when blight is not paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cinder Strike")
                    .withCardOnBattlefield(1, "Grizzly Bears") // available creature, but won't blight
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 target — survives 2 damage
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyBears = game.findPermanent("Grizzly Bears")!!
                val hillGiant = game.findPermanent("Hill Giant")!!

                val cinderStrike = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Cinder Strike"
                }

                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        cinderStrike,
                        listOf(ChosenTarget.Permanent(hillGiant))
                        // No additionalCostPayment → blight skipped
                    )
                )
                withClue("Cinder Strike (no blight) should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Grizzly Bears should not have any counters (blight wasn't paid)
                val counters = game.state.getEntity(grizzlyBears)?.get<CountersComponent>()
                withClue("Grizzly Bears should have no -1/-1 counters") {
                    (counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
                }

                game.resolveStack()

                // Hill Giant (3/3) takes 2 damage → survives
                withClue("Hill Giant should still be on the battlefield (only took 2 damage)") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("can cast without blight when controller has no creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cinder Strike")
                    .withCardOnBattlefield(2, "Grizzly Bears") // P2's creature — target
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyBears = game.findPermanent("Grizzly Bears")!!

                val cinderStrike = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Cinder Strike"
                }

                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        cinderStrike,
                        listOf(ChosenTarget.Permanent(grizzlyBears))
                    )
                )
                withClue("Cinder Strike should be castable with no creatures to blight: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Grizzly Bears (2/2) takes 2 damage → dies
                withClue("Grizzly Bears should be in graveyard after taking 2 damage") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
