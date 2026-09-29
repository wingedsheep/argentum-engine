package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chomping Kavu — backup 1; "can't be blocked by creatures with power 2 or less" is printed on the
 * Kavu and, when backup targets another creature, granted to it until end of turn.
 */
class ChompingKavuScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castAndBackup(targetBears: Boolean): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Chomping Kavu")
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Savannah Lions")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Chomping Kavu").error shouldBe null
        game.resolveStack()
        val target = if (targetBears) game.findPermanent("Grizzly Bears")!! else game.findPermanent("Chomping Kavu")!!
        game.selectTargets(listOf(target)).error shouldBe null
        game.resolveStack()
        return game
    }

    private fun attackWithBears(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
    }

    init {
        context("Chomping Kavu") {
            test("backup on another creature: counter plus the evasion, which stops a power-2 blocker") {
                val game = castAndBackup(targetBears = true)
                plusOnes(game, game.findPermanent("Grizzly Bears")!!) shouldBe 1
                attackWithBears(game)
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldNotBe null
            }

            test("the granted evasion still lets a power-3 creature block") {
                val game = castAndBackup(targetBears = true)
                attackWithBears(game)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
            }

            test("backup on itself: counter only, the other creature gains nothing") {
                val game = castAndBackup(targetBears = false)
                plusOnes(game, game.findPermanent("Chomping Kavu")!!) shouldBe 1
                attackWithBears(game)
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldBe null
            }

            test("printed evasion: a power-2 creature can't block the Kavu, a power-3 one can") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Chomping Kavu")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Chomping Kavu" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Chomping Kavu"))).error shouldNotBe null
                game.declareBlockers(mapOf("Hill Giant" to listOf("Chomping Kavu"))).error shouldBe null
            }
        }
    }
}
