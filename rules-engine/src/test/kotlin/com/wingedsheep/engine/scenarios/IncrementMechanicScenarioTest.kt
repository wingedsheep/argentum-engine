package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Increment keyword (Secrets of Strixhaven), exercised through
 * Cuboid Colony (1/1) and Hungry Graffalon (3/4).
 *
 * Increment: "Whenever you cast a spell, if the amount of mana you spent is greater than this
 * creature's power or toughness, put a +1/+1 counter on this creature." The threshold is the
 * *smaller* of power/toughness (`min`) — "greater than its power or toughness" is satisfied as soon
 * as the mana spent beats the smaller stat — and it is read from the creature's projected stats, so
 * the bar rises as it grows. The mana compared is the mana spent on the *triggering* spell.
 */
class IncrementMechanicScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int {
        val id = findPermanent(name) ?: return -1
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Cuboid Colony (1/1) — threshold is min(power, toughness), checked against projected stats") {

            test("equal mana adds nothing, greater mana adds a counter, and the bar then rises") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cuboid Colony")
                    .withCardInHand(1, "Llanowar Elves")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // 1-mana spell: 1 is NOT > min(1, 1) = 1 -> no counter.
                game.castSpell(1, "Llanowar Elves")
                game.resolveStack()
                withClue("a 1-mana spell must not grow a 1/1 (1 is not > 1)") {
                    game.plusCounters("Cuboid Colony") shouldBe 0
                }

                // 2-mana spell: 2 > min(1, 1) = 1 -> +1/+1 counter (now a 2/2).
                game.castSpell(1, "Grizzly Bears")
                game.resolveStack()
                withClue("a 2-mana spell must grow the 1/1 to 2/2") {
                    game.plusCounters("Cuboid Colony") shouldBe 1
                }

                // Now a 2/2: another 2-mana spell is NOT > min(2, 2) = 2 -> the bar has risen.
                // This only passes if the intervening-if reads the creature's PROJECTED P/T (with
                // the counter), not its printed 1/1.
                game.castSpell(1, "Grizzly Bears")
                game.resolveStack()
                withClue("once it is a 2/2, another 2-mana spell must not grow it (2 is not > 2)") {
                    game.plusCounters("Cuboid Colony") shouldBe 1
                }
            }
        }

        context("Hungry Graffalon (3/4) — 'power or toughness' means the smaller stat") {

            test("a 3-mana spell adds nothing but a 4-mana spell does (threshold = power 3, not toughness 4)") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hungry Graffalon")
                    .withCardInHand(1, "Trained Armodon")
                    .withCardInHand(1, "Giant Spider")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // 3 mana ({1}{G}{G}): 3 is NOT > min(3, 4) = 3 -> no counter (the bar is the smaller stat).
                game.castSpell(1, "Trained Armodon")
                game.resolveStack()
                withClue("a 3-mana spell must not grow a 3/4 (3 is not > min(3, 4) = 3)") {
                    game.plusCounters("Hungry Graffalon") shouldBe 0
                }

                // 4 mana ({3}{G}): 4 > min(3, 4) = 3 -> counter. If the threshold were max = 4 this
                // would add nothing, so this pins down the min semantics.
                game.castSpell(1, "Giant Spider")
                game.resolveStack()
                withClue("a 4-mana spell must grow the 3/4 (4 > min(3, 4) = 3)") {
                    game.plusCounters("Hungry Graffalon") shouldBe 1
                }
            }
        }
    }
}
