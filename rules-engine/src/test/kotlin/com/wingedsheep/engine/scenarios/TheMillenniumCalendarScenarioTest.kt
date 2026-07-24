package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.TheMillenniumCalendar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Millennium Calendar (LCI #257) — {1} Legendary Artifact, Mythic.
 *
 * "Whenever you untap one or more permanents during your untap step, put that many time counters
 *  on The Millennium Calendar.
 *  {2}, {T}: Double the number of time counters on The Millennium Calendar.
 *  When there are 1,000 or more time counters on The Millennium Calendar, sacrifice it and each
 *  opponent loses 1,000 life."
 *
 * The headline is the new **batch untap** trigger ([com.wingedsheep.sdk.dsl.Triggers.OneOrMoreBecomeUntapped]):
 * the untap step untaps all your permanents at once but the ability fires a *single* time, and the
 * count of untapped permanents is read via the trigger's captured collection ("put that many").
 * The doubler reuses `DoubleCounters`; the 1,000-counter kill is a CR 603.8 state-triggered ability.
 */
class TheMillenniumCalendarScenarioTest : ScenarioTestBase() {

    private fun TestGame.timeCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    init {
        context("The Millennium Calendar — untap batch trigger") {

            test("untapping three permanents in your untap step adds exactly three time counters") {
                var builder = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "The Millennium Calendar") // untapped — won't self-count
                    .withCardOnBattlefield(1, "Mountain", tapped = true)
                    .withCardOnBattlefield(1, "Mountain", tapped = true)
                    .withCardOnBattlefield(1, "Mountain", tapped = true)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(4) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(4) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val calendar = game.findPermanent("The Millennium Calendar").shouldNotBeNull()

                // Advance into the Calendar controller's (player 1's) own untap step, which untaps
                // the three tapped Mountains as one batch, then resolve the queued trigger in upkeep.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("one batch trigger fires once with count = 3 permanents untapped") {
                    game.timeCounters(calendar) shouldBe 3
                }
            }
        }

        context("The Millennium Calendar — doubling and the 1,000-counter kill") {

            test("{2}, {T} doubles the number of time counters") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all)
                driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

                val player1 = driver.activePlayer!!
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

                val calendar = driver.putPermanentOnBattlefield(player1, "The Millennium Calendar")
                driver.addComponent(calendar, CountersComponent(mapOf(CounterType.TIME to 5)))
                driver.giveMana(player1, Color.RED, 2)

                val doubleAbility = TheMillenniumCalendar.activatedAbilities.first().id
                driver.submitSuccess(ActivateAbility(player1, calendar, doubleAbility))
                driver.bothPass()

                withClue("doubling 5 time counters yields 10") {
                    driver.state.getEntity(calendar)?.get<CountersComponent>()
                        ?.getCount(CounterType.TIME) shouldBe 10
                }
            }

            test("at 1,000 time counters it sacrifices itself and each opponent loses 1,000 life") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all)
                // High starting life so the drained opponent survives (no game-over mid-test),
                // isolating the "loses exactly 1,000 life" assertion.
                driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 1005)

                val player1 = driver.activePlayer!!
                val opponent = driver.getOpponent(player1)
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

                val calendar = driver.putPermanentOnBattlefield(player1, "The Millennium Calendar")
                driver.addComponent(calendar, CountersComponent(mapOf(CounterType.TIME to 1000)))

                // The state-triggered ability fires on the next state-trigger poll; advancing priority
                // detects it, puts it on the stack, and resolves it.
                var guard = 0
                while (driver.findPermanent(player1, "The Millennium Calendar") != null && guard++ < 12) {
                    driver.bothPass()
                }

                withClue("the Calendar sacrifices itself once the threshold is met") {
                    driver.findPermanent(player1, "The Millennium Calendar") shouldBe null
                }
                withClue("each opponent loses exactly 1,000 life (life loss, not damage)") {
                    driver.getLifeTotal(opponent) shouldBe 5
                }
                withClue("the Calendar's controller loses no life") {
                    driver.getLifeTotal(player1) shouldBe 1005
                }
            }
        }
    }
}
