package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Rakdos, the Showstopper — "When Rakdos enters, flip a coin for each creature that isn't a Demon,
 * Devil, or Imp. Destroy each creature whose coin comes up tails."
 *
 * Coins are random, so each seed asserts the invariants: one flip per eligible creature (none for
 * Rakdos or the Demon/Devil/Imp), exactly the lost flips' worth of eligible creatures destroyed, and
 * every destruction after the last flip (one simultaneous destroy, not a flip-then-destroy loop).
 * Across the seeds both a survivor and a casualty must appear, so the result is per creature.
 */
class RakdosTheShowstopperScenarioTest : ScenarioTestBase() {

    private val eligible = listOf("Savannah Lions", "Centaur Courser", "Grizzly Bears", "Hill Giant")
    private val exempt = listOf("Rakdos, the Showstopper", "Lord of the Pit", "Stone-Throwing Devils", "Bog Imp")

    init {
        context("Rakdos, the Showstopper") {

            test("flips once per non-Demon/Devil/Imp creature and destroys exactly the tails, together") {
                var sawSurvivor = false
                var sawCasualty = false
                listOf(1L, 7L, 42L, 1234L, 99L).forEach { seed ->
                    val game = scenario()
                        .withRngSeed(seed)
                        .withPlayers("Player1", "Player2")
                        .withCardInHand(1, "Rakdos, the Showstopper")
                        .withLandsOnBattlefield(1, "Swamp", 3)
                        .withLandsOnBattlefield(1, "Mountain", 3)
                        .withCardOnBattlefield(1, "Savannah Lions")
                        .withCardOnBattlefield(1, "Centaur Courser")
                        .withCardOnBattlefield(1, "Stone-Throwing Devils")
                        .withCardOnBattlefield(2, "Grizzly Bears")
                        .withCardOnBattlefield(2, "Hill Giant")
                        .withCardOnBattlefield(2, "Lord of the Pit")
                        .withCardOnBattlefield(2, "Bog Imp")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    game.castSpell(1, "Rakdos, the Showstopper").error shouldBe null
                    val events = game.resolveStack().flatMap { it.events }

                    val flips = events.filterIsInstance<CoinFlipEvent>()
                    val destroyed = events.filterIsInstance<ZoneChangeEvent>()
                        .filter { it.fromZone == Zone.BATTLEFIELD && it.toZone == Zone.GRAVEYARD }

                    withClue("seed $seed: one coin per eligible creature") {
                        flips.size shouldBe eligible.size
                    }
                    withClue("seed $seed: Rakdos and the Demon, Devil and Imp are never at risk") {
                        exempt.forEach { game.isOnBattlefield(it) shouldBe true }
                    }
                    withClue("seed $seed: exactly the tails were destroyed") {
                        destroyed.size shouldBe flips.count { !it.won }
                        eligible.count { !game.isOnBattlefield(it) } shouldBe flips.count { !it.won }
                    }
                    withClue("seed $seed: every destruction comes after the last flip") {
                        val lastFlip = events.indexOfLast { it is CoinFlipEvent }
                        destroyed.forEach { events.indexOf(it) shouldBeGreaterThan lastFlip }
                    }
                    val survivors = eligible.count { game.isOnBattlefield(it) }
                    if (survivors > 0) sawSurvivor = true
                    if (survivors < eligible.size) sawCasualty = true
                }
                withClue("across seeds, some creatures survive and some die: the coin is per creature") {
                    sawSurvivor shouldBe true
                    sawCasualty shouldBe true
                }
            }
        }
    }
}
