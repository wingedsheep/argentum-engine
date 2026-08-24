package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Unbreathing Horde — {2}{B} 0/0 Zombie that enters with a +1/+1 counter for each
 * other Zombie you control and each Zombie card in your graveyard, and whose damage clause is
 * "prevent that damage and remove a +1/+1 counter from it".
 *
 * Two things worth pinning: the two-zone entry count (including the "**other**" exclusion), and the
 * printed rulings on the prevention — one counter per damage event however large, and prevention
 * even with no counter left to spend.
 */
class UnbreathingHordeScenarioTest : ScenarioTestBase() {

    init {
        context("Unbreathing Horde") {

            fun plusOneCounters(game: TestGame, name: String): Int =
                game.findPermanent(name)?.let { id ->
                    game.state.getEntity(id)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
                } ?: 0

            test("it counts other Zombies you control and Zombie cards in your graveyard") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Unbreathing Horde")
                    .withCardOnBattlefield(1, "Diregraf Ghoul")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Diregraf Ghoul")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .build()

                val cast = game.castSpell(1, "Unbreathing Horde")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("one battlefield Zombie + one graveyard Zombie card; the Bear and the Bolt don't count") {
                    plusOneCounters(game, "Unbreathing Horde") shouldBe 2
                }
                withClue("a 0/0 with two counters is a 2/2") {
                    val horde = game.findPermanent("Unbreathing Horde")!!
                    game.state.projectedState.getPower(horde) shouldBe 2
                    game.state.projectedState.getToughness(horde) shouldBe 2
                }
            }

            test("'each OTHER Zombie' excludes the Horde itself") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Unbreathing Horde")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .build()

                game.castSpell(1, "Unbreathing Horde").error shouldBe null
                game.resolveStack()

                withClue("no other Zombie anywhere, so it enters with nothing") {
                    plusOneCounters(game, "Unbreathing Horde") shouldBe 0
                }
            }

            test("damage is prevented and exactly one counter is removed, however large the damage") {
                val game = scenario()
                    .withPlayers()
                    // Three graveyard Zombie cards => it enters as a 3/3.
                    .withCardInHand(1, "Unbreathing Horde")
                    .withCardInGraveyard(1, "Diregraf Ghoul")
                    .withCardInGraveyard(1, "Diregraf Ghoul")
                    .withCardInGraveyard(1, "Diregraf Ghoul")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()

                game.castSpell(1, "Unbreathing Horde").error shouldBe null
                game.resolveStack()
                plusOneCounters(game, "Unbreathing Horde") shouldBe 3

                val horde = game.findPermanent("Unbreathing Horde")!!
                val bolt = game.castSpell(1, "Lightning Bolt", targetId = horde)
                withClue("the Bolt should be castable: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()

                withClue("3 damage prevented, one counter spent (the printed ruling)") {
                    plusOneCounters(game, "Unbreathing Horde") shouldBe 2
                }
                withClue("it survives — the damage never happened") {
                    game.isOnBattlefield("Unbreathing Horde") shouldBe true
                }
            }

            test("with no counters left the damage is still prevented") {
                val game = scenario()
                    .withPlayers()
                    // No other Zombies, so the Horde enters with zero counters. Glorious Anthem is
                    // already out to hold the 0/0's toughness above 0, so it survives the entry SBA
                    // and the prevention clause is reachable with an empty counter pile.
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardInHand(1, "Unbreathing Horde")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()

                game.castSpell(1, "Unbreathing Horde").error shouldBe null
                game.resolveStack()

                withClue("the anthem keeps the counterless 0/0 alive") {
                    game.isOnBattlefield("Unbreathing Horde") shouldBe true
                }
                plusOneCounters(game, "Unbreathing Horde") shouldBe 0

                val horde = game.findPermanent("Unbreathing Horde")!!
                game.castSpell(1, "Lightning Bolt", targetId = horde).error shouldBe null
                game.resolveStack()

                withClue("prevention does not depend on having a counter to remove") {
                    game.isOnBattlefield("Unbreathing Horde") shouldBe true
                    plusOneCounters(game, "Unbreathing Horde") shouldBe 0
                }
            }

            test("combat damage is prevented too") {
                val game = scenario()
                    .withPlayers()
                    // Placed directly, so the entry replacement never ran and the Horde has no
                    // counters — Glorious Anthem is what keeps the 0/0 alive to block. That also
                    // makes this the combat-path version of the "no counter to spend" case.
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withCardOnBattlefield(2, "Unbreathing Horde")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Unbreathing Horde" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("the Giant's 3 combat damage is prevented, so the 1/1 blocker lives") {
                    game.isOnBattlefield("Unbreathing Horde") shouldBe true
                }
                withClue("the attack was blocked, so nothing reached the player") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
