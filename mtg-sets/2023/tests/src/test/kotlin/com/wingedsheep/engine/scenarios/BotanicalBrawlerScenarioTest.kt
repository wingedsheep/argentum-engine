package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Botanical Brawler (March of the Machine #220).
 *
 * {G}{W} 0/0 Trample. Enters with two +1/+1 counters. "Whenever one or more +1/+1 counters are put
 * on another permanent you control, if it's the first time +1/+1 counters have been put on that
 * permanent this turn, put a +1/+1 counter on this creature."
 *
 * Sage of the Fang ("put a +1/+1 counter on target creature" on entry) is the +1/+1 source, Scar
 * the -1/-1 source that proves the first-time window is per counter kind.
 */
class BotanicalBrawlerScenarioTest : ScenarioTestBase() {

    private fun plusOne(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Brawler is a 0/0 that needs its entry counters, so it is cast rather than placed. */
    private fun castBrawler(game: TestGame): EntityId {
        val cast = game.castSpell(1, "Botanical Brawler")
        withClue("Casting Botanical Brawler should succeed: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()
        return game.findPermanent("Botanical Brawler")!!
    }

    private fun castSage(game: TestGame, target: EntityId) {
        val cast = game.castSpell(1, "Sage of the Fang")
        withClue("Casting Sage of the Fang should succeed: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()
        game.selectTargets(listOf(target))
        game.resolveStack()
    }

    init {
        context("Botanical Brawler") {

            test("enters with two +1/+1 counters and doesn't trigger on its own") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Botanical Brawler")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Botanical Brawler").error shouldBe null
                game.resolveStack()

                plusOne(game, game.findPermanent("Botanical Brawler")!!) shouldBe 2
            }

            test("the first +1/+1 counter on another creature you control grows it; a second doesn't") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Botanical Brawler")
                    .withLandsOnBattlefield(1, "Savannah", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Sage of the Fang")
                    .withCardInHand(1, "Sage of the Fang")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val brawler = castBrawler(game)
                val bears = game.findPermanent("Grizzly Bears")!!
                val before = plusOne(game, brawler)

                castSage(game, bears)
                withClue("first +1/+1 counters on Grizzly Bears this turn trigger Brawler") {
                    plusOne(game, brawler) shouldBe before + 1
                }

                castSage(game, bears)
                withClue("a second +1/+1 placement on the same permanent this turn doesn't") {
                    plusOne(game, brawler) shouldBe before + 1
                }
            }

            test("a -1/-1 counter earlier in the turn doesn't close the +1/+1 window") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Botanical Brawler")
                    .withLandsOnBattlefield(1, "Savannah", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Scar")
                    .withCardInHand(1, "Sage of the Fang")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val brawler = castBrawler(game)
                val bears = game.findPermanent("Grizzly Bears")!!
                val before = plusOne(game, brawler)

                val scar = game.castSpell(1, "Scar", bears)
                withClue("Casting Scar should succeed: ${scar.error}") { scar.error shouldBe null }
                game.resolveStack()
                withClue("a -1/-1 counter is not a +1/+1 counter") {
                    plusOne(game, brawler) shouldBe before
                }

                castSage(game, bears)
                withClue("the first +1/+1 counter on Grizzly Bears still triggers Brawler") {
                    plusOne(game, brawler) shouldBe before + 1
                }
            }

            test("another permanent entering with +1/+1 counters triggers it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Botanical Brawler")
                    .withLandsOnBattlefield(1, "Savannah", 2)
                    .withCardInHand(1, "Dockworker Drone")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val brawler = castBrawler(game)
                val before = plusOne(game, brawler)

                game.castSpell(1, "Dockworker Drone").error shouldBe null
                game.resolveStack()

                plusOne(game, brawler) shouldBe before + 1
            }

            test("counters on an opponent's creature don't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Botanical Brawler")
                    .withLandsOnBattlefield(1, "Savannah", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Sage of the Fang")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val brawler = castBrawler(game)
                val before = plusOne(game, brawler)

                castSage(game, game.findPermanent("Grizzly Bears")!!)

                plusOne(game, brawler) shouldBe before
            }
        }
    }
}
