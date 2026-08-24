package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Dearly Departed — {4}{W}{W} 5/5 Spirit with flying and "As long as this
 * creature is in your graveyard, each Human creature you control enters with an additional +1/+1
 * counter on it."
 *
 * This is the first card whose replacement effect functions from the **graveyard**
 * (`activeZones = {GRAVEYARD}`), so the tests pin both directions of that switch: live from the
 * graveyard, inert on the battlefield.
 */
class DearlyDepartedScenarioTest : ScenarioTestBase() {

    init {
        context("Dearly Departed") {

            fun plusOneCounters(game: TestGame, name: String): Int =
                game.findPermanent(name)?.let { id ->
                    game.state.getEntity(id)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
                } ?: 0

            test("from the graveyard, a Human creature you control enters with an extra +1/+1 counter") {
                val game = scenario()
                    .withPlayers()
                    .withCardInGraveyard(1, "Dearly Departed")
                    .withCardInHand(1, "Doomed Traveler")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .build()

                val cast = game.castSpell(1, "Doomed Traveler")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("the 1/1 Human Soldier entered with one +1/+1 counter") {
                    plusOneCounters(game, "Doomed Traveler") shouldBe 1
                }
                withClue("and is therefore a 2/2") {
                    val traveler = game.findPermanent("Doomed Traveler")!!
                    game.state.projectedState.getPower(traveler) shouldBe 2
                    game.state.projectedState.getToughness(traveler) shouldBe 2
                }
            }

            test("on the battlefield it does nothing — the effect is scoped to the graveyard") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Dearly Departed")
                    .withCardInHand(1, "Doomed Traveler")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .build()

                game.castSpell(1, "Doomed Traveler").error shouldBe null
                game.resolveStack()

                withClue("`activeZones = {GRAVEYARD}` switches the grant off on the battlefield") {
                    plusOneCounters(game, "Doomed Traveler") shouldBe 0
                }
            }

            test("the effect is cumulative — two copies in the graveyard hand out two counters") {
                val game = scenario()
                    .withPlayers()
                    .withCardInGraveyard(1, "Dearly Departed")
                    .withCardInGraveyard(1, "Dearly Departed")
                    .withCardInHand(1, "Doomed Traveler")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .build()

                game.castSpell(1, "Doomed Traveler").error shouldBe null
                game.resolveStack()

                withClue("the printed ruling: one counter per Dearly Departed in your graveyard") {
                    plusOneCounters(game, "Doomed Traveler") shouldBe 2
                }
            }

            test("a non-Human creature gets nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardInGraveyard(1, "Dearly Departed")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Bear is not Human") {
                    plusOneCounters(game, "Grizzly Bears") shouldBe 0
                }
            }

            test("a Human an opponent controls gets nothing — 'you' is the graveyard's owner") {
                val game = scenario()
                    .withPlayers()
                    .withCardInGraveyard(1, "Dearly Departed")
                    .withCardInHand(2, "Doomed Traveler")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withActivePlayer(2)
                    .build()

                game.castSpell(2, "Doomed Traveler").error shouldBe null
                game.resolveStack()

                withClue("the filter is 'each Human creature YOU control'") {
                    plusOneCounters(game, "Doomed Traveler") shouldBe 0
                }
            }
        }
    }
}
