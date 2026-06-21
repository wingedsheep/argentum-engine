package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lost Isle Calling (LTR #61).
 *
 * "Whenever you scry, put a verse counter on this enchantment.
 *  {4}{U}{U}, Exile this enchantment: Draw a card for each verse counter on this enchantment.
 *  If it had seven or more verse counters on it, take an extra turn after this one.
 *  Activate only as a sorcery."
 *
 * The activated ability exiles its own source as a cost (CR 122.2 wipes the counters on the zone
 * change), so the draw amount and the seven-or-more test read the pre-cost count snapshotted into
 * the resolution context — `DynamicAmount.LastKnownSourceCounters`.
 */
class LostIsleCallingScenarioTest : ScenarioTestBase() {

    // Substrate "Scry 1" sorcery so the test can drive a real scry → "Whenever you scry" trigger.
    private val scryOne = card("Test Scry One") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Scry 1."
        spell { effect = Patterns.Library.scry(1) }
    }

    init {
        cardRegistry.register(scryOne)

        context("Lost Isle Calling") {

            fun ScenarioBuilder.withLibrary(playerNumber: Int, cardName: String, count: Int): ScenarioBuilder {
                repeat(count) { withCardInLibrary(playerNumber, cardName) }
                return this
            }

            fun TestGame.setVerseCounters(id: EntityId, n: Int) {
                state = state.updateEntity(id) { c ->
                    c.with(CountersComponent(mapOf(CounterType.VERSE to n)))
                }
            }

            fun TestGame.verseCounters(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.VERSE) ?: 0

            // Resolve a scry's "put any number on bottom / reorder top" decisions, keeping order.
            fun TestGame.resolveScryKeepingOrder() {
                var guard = 0
                while (hasPendingDecision() && guard++ < 6) {
                    when (val d = getPendingDecision()) {
                        is SelectCardsDecision -> skipSelection()
                        is ReorderLibraryDecision -> submitDecision(OrderedResponse(d.id, d.cards))
                        else -> return
                    }
                }
            }

            test("scry puts a verse counter on Lost Isle Calling") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lost Isle Calling")
                    .withCardInHand(1, "Test Scry One")
                    .withLibrary(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val isle = game.findPermanent("Lost Isle Calling")!!
                game.verseCounters(isle) shouldBe 0

                game.castSpell(1, "Test Scry One")
                game.resolveStack()           // resolve the scry sorcery
                game.resolveScryKeepingOrder() // drive the scry decisions
                game.resolveStack()           // resolve the "Whenever you scry" trigger

                withClue("Scrying once adds exactly one verse counter") {
                    game.verseCounters(isle) shouldBe 1
                }
            }

            test("activating with N verse counters draws N cards and exiles the enchantment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lost Isle Calling")
                    .withLandsOnBattlefield(1, "Island", 6) // {4}{U}{U}
                    .withLibrary(1, "Island", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val isle = game.findPermanent("Lost Isle Calling")!!
                game.setVerseCounters(isle, 3)

                val handBefore = game.handSize(1)
                val ability = cardRegistry.getCard("Lost Isle Calling")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = isle, abilityId = ability.id)
                )
                withClue("Activating Lost Isle Calling should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The enchantment is exiled as part of the cost") {
                    game.isOnBattlefield("Lost Isle Calling") shouldBe false
                }
                withClue("Draws a card for each of the 3 pre-exile verse counters") {
                    game.handSize(1) shouldBe handBefore + 3
                }
                withClue("Fewer than seven counters: no extra turn (opponent is not set to skip)") {
                    game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe false
                }
            }

            test("activating with seven or more verse counters grants an extra turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lost Isle Calling")
                    .withLandsOnBattlefield(1, "Island", 6) // {4}{U}{U}
                    .withLibrary(1, "Island", 12)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val isle = game.findPermanent("Lost Isle Calling")!!
                game.setVerseCounters(isle, 7)

                val handBefore = game.handSize(1)
                val ability = cardRegistry.getCard("Lost Isle Calling")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = isle, abilityId = ability.id)
                )
                withClue("Activating Lost Isle Calling should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Draws a card for each of the 7 pre-exile verse counters") {
                    game.handSize(1) shouldBe handBefore + 7
                }
                withClue("Seven or more counters: Player 1 takes an extra turn, so Player 2 skips") {
                    game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
                    game.state.getEntity(game.player1Id)?.has<SkipNextTurnComponent>() shouldBe false
                }
            }
        }
    }
}
