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
 * Zar Ojanen, Scion of Efrava (DMU #227) — {3}{G}{W} 4/4 Legendary Cat Warrior.
 *
 * "Domain — Whenever Zar Ojanen becomes tapped, put a +1/+1 counter on each creature you control
 * with toughness less than the number of basic land types among lands you control."
 *
 * The group filter compares each creature's *projected* toughness against domain, evaluated at
 * resolution for the ability's controller. "Each creature you control" includes Zar itself.
 */
class ZarOjanenScionOfEfravaScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Zar Ojanen, Scion of Efrava") {

            test("attacking taps Zar: counters go only on your creatures with projected toughness below domain") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zar Ojanen, Scion of Efrava", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Contaminated Aquifer") // Land — Island Swamp
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zar = game.findPermanent("Zar Ojanen, Scion of Efrava")!!
                val allBears = game.findPermanents("Grizzly Bears")
                val bears = allBears.first { game.state.projectedState.getController(it) == game.player1Id }
                val opposingBears = allBears.first { game.state.projectedState.getController(it) == game.player2Id }
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Zar Ojanen, Scion of Efrava" to 2)).error shouldBe null
                game.resolveStack()

                withClue("Bears are 3/3 under the Anthem, below domain 4 → one counter") {
                    plusOnes(game, bears) shouldBe 1
                }
                withClue("Hill Giant is 4/4 under the Anthem (printed 3) — not below 4") {
                    plusOnes(game, giant) shouldBe 0
                }
                withClue("Zar is 5/5 under the Anthem — not below 4") {
                    plusOnes(game, zar) shouldBe 0
                }
                withClue("an opponent's creature never qualifies") {
                    plusOnes(game, opposingBears) shouldBe 0
                }
            }

            test("Zar counts itself when its toughness is below domain") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zar Ojanen, Scion of Efrava", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Contaminated Aquifer") // Land — Island Swamp
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zar = game.findPermanent("Zar Ojanen, Scion of Efrava")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Zar Ojanen, Scion of Efrava" to 2)).error shouldBe null
                game.resolveStack()

                withClue("five basic land types, Zar is 4/4 → one counter on itself") {
                    plusOnes(game, zar) shouldBe 1
                }
            }
        }
    }
}
