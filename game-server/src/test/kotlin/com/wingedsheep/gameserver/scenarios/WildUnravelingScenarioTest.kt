package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Scenario tests for Wild Unraveling.
 *
 * Card reference:
 * - Wild Unraveling ({U}{U}): Instant
 *   "As an additional cost to cast this spell, blight 2 or pay {1}.
 *    Counter target spell."
 */
class WildUnravelingScenarioTest : ScenarioTestBase() {

    init {
        context("Wild Unraveling - blight path") {

            test("counters target spell when blight 2 is paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Wild Unraveling")
                    .withCardOnBattlefield(2, "Glory Seeker") // creature to blight
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Island", 2) // only 2 islands = can't afford {1}{U}{U} but can blight
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Grizzly Bears
                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Grizzly Bears should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Find spell on stack
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                // Find Glory Seeker for blight target
                val glorySeeker = game.findPermanent("Glory Seeker")!!

                // Player 2 casts Wild Unraveling with blight 2
                val hand = game.state.getHand(game.player2Id)
                val wildUnravelingId = hand.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Wild Unraveling"
                }
                val counterResult = game.execute(
                    CastSpell(
                        game.player2Id,
                        wildUnravelingId,
                        listOf(ChosenTarget.Spell(spellOnStack)),
                        additionalCostPayment = AdditionalCostPayment(
                            blightTargets = listOf(glorySeeker)
                        )
                    )
                )
                withClue("Wild Unraveling (blight path) should be cast: ${counterResult.error}") {
                    counterResult.error shouldBe null
                }

                // Glory Seeker should have 2 -1/-1 counters after blight payment
                val counters = game.state.getEntity(glorySeeker)?.get<CountersComponent>()
                withClue("Glory Seeker should have 2 -1/-1 counters") {
                    counters.shouldNotBeNull()
                    counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
                }

                // Resolve the stack
                game.resolveStack()

                // Grizzly Bears should be countered
                withClue("Grizzly Bears should be in graveyard (countered)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }

        context("Wild Unraveling - pay mana path") {

            test("counters target spell when {1} is paid instead of blight") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Wild Unraveling")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Island", 3) // 3 islands = enough for {1}{U}{U}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Grizzly Bears
                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Grizzly Bears should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Find spell on stack
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                // Player 2 casts Wild Unraveling without blight (pay path = {1}{U}{U})
                val hand = game.state.getHand(game.player2Id)
                val wildUnravelingId = hand.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Wild Unraveling"
                }
                val counterResult = game.execute(
                    CastSpell(
                        game.player2Id,
                        wildUnravelingId,
                        listOf(ChosenTarget.Spell(spellOnStack))
                        // No additionalCostPayment → pay mana path
                    )
                )
                withClue("Wild Unraveling (pay path) should be cast: ${counterResult.error}") {
                    counterResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Grizzly Bears should be countered
                withClue("Grizzly Bears should be in graveyard (countered)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("cannot cast with pay path when not enough mana for {1}{U}{U}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Wild Unraveling")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Island", 2) // only 2 islands, not enough for {1}{U}{U}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Grizzly Bears
                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Grizzly Bears should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Find spell on stack
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                // Player 2 tries Wild Unraveling without blight (pay path = {1}{U}{U})
                // but only has 2 Islands = can't pay {1}{U}{U}
                val hand = game.state.getHand(game.player2Id)
                val wildUnravelingId = hand.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Wild Unraveling"
                }
                val counterResult = game.execute(
                    CastSpell(
                        game.player2Id,
                        wildUnravelingId,
                        listOf(ChosenTarget.Spell(spellOnStack))
                    )
                )
                withClue("Wild Unraveling (pay path) should fail with only 2 Islands") {
                    counterResult.error.shouldNotBeNull()
                }
            }
        }
    }
}
