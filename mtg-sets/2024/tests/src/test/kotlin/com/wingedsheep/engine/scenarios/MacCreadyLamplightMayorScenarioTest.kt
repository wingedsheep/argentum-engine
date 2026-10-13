package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * MacCready, Lamplight Mayor (PIP).
 *
 * - "Whenever a creature you control with power 2 or less attacks, it gains skulk until end of
 *   turn." Skulk is granted as its rules meaning — `CantBeBlockedBy(power greater than the
 *   attacker)` — so the comparison must be made against the *attacker's* power, not MacCready's.
 * - "Whenever a creature with power 4 or greater attacks you, its controller loses 2 life and you
 *   gain 2 life." The `attackingPlayer(Controller)` trigger filter must fire only for big
 *   creatures attacking MacCready's controller.
 */
class MacCreadyLamplightMayorScenarioTest : ScenarioTestBase() {

    private fun creature(name: String, power: Int, toughness: Int) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        this.power = power
        this.toughness = toughness
    }

    private val smallAttacker = creature("MacCready Test Scout", 2, 2)
    private val midAttacker = creature("MacCready Test Raider", 3, 3)
    private val bigAttacker = creature("MacCready Test Brute", 4, 4)
    private val evenBlocker = creature("MacCready Test Guard", 2, 2)
    private val biggerBlocker = creature("MacCready Test Wall", 3, 5)
    private val hugeBlocker = creature("MacCready Test Behemoth", 5, 5)

    /** Player 1 attacks player 2 with [attacker] beside a non-attacking MacCready; returns the block result. */
    private fun blockAttempt(attacker: String, blocker: String): ExecutionResult {
        val game = scenario()
            .withPlayers("You", "Opponent")
            .withCardOnBattlefield(1, "MacCready, Lamplight Mayor")
            .withCardOnBattlefield(1, attacker)
            .withCardOnBattlefield(2, blocker)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf(attacker to 2)).error shouldBe null
        game.resolveStack()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game.declareBlockers(mapOf(blocker to listOf(attacker)))
    }

    init {
        listOf(smallAttacker, midAttacker, bigAttacker, evenBlocker, biggerBlocker, hugeBlocker)
            .forEach { cardRegistry.register(it) }

        context("skulk for your power-2-or-less attackers") {
            test("a creature with greater power than the attacker can't block it") {
                withClue("3-power blocker vs 2-power skulker") {
                    blockAttempt(smallAttacker.name, biggerBlocker.name).error shouldNotBe null
                }
            }

            test("a creature with equal power can still block") {
                blockAttempt(smallAttacker.name, evenBlocker.name).error shouldBe null
            }

            test("a power-3 attacker doesn't trigger it, so a bigger creature can block") {
                blockAttempt(midAttacker.name, hugeBlocker.name).error shouldBe null
            }
        }

        context("drain from power-4-or-greater attackers") {
            fun opponentAttacks(attacker: String): TestGame {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "MacCready, Lamplight Mayor")
                    .withCardOnBattlefield(2, attacker)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf(attacker to 1)).error shouldBe null
                game.resolveStack()
                return game
            }

            test("a 4-power creature attacking you: its controller loses 2, you gain 2") {
                val game = opponentAttacks(bigAttacker.name)
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }

            test("a 3-power creature attacking you doesn't trigger it") {
                val game = opponentAttacks(midAttacker.name)
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }

            test("your own 4-power creature attacking an opponent doesn't trigger it") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "MacCready, Lamplight Mayor")
                    .withCardOnBattlefield(1, bigAttacker.name)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf(bigAttacker.name to 2)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
