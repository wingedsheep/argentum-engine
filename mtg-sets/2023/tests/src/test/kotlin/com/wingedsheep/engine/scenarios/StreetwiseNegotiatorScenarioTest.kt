package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Streetwise Negotiator — backup 1; "assigns combat damage equal to its toughness rather than its
 * power" is printed on the Negotiator and, when backup targets another creature, granted to it
 * until end of turn.
 */
class StreetwiseNegotiatorScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castAndBackup(targetOther: Boolean): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Streetwise Negotiator")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardOnBattlefield(1, "Bill the Pony") // 1/4
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Streetwise Negotiator").error shouldBe null
        game.resolveStack()
        val target = if (targetOther) game.findPermanent("Bill the Pony")!! else game.findPermanent("Streetwise Negotiator")!!
        game.selectTargets(listOf(target)).error shouldBe null
        game.resolveStack()
        return game
    }

    private fun attackWith(game: TestGame, name: String) {
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf(name to 2)).error shouldBe null
        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
    }

    init {
        context("Streetwise Negotiator") {
            test("backup on another creature: counter plus the toughness-damage ability") {
                val game = castAndBackup(targetOther = true)
                plusOnes(game, game.findPermanent("Bill the Pony")!!) shouldBe 1
                // Bill is now 2/5 and assigns 5, not 2.
                attackWith(game, "Bill the Pony")
                game.getLifeTotal(2) shouldBe 15
            }

            test("backup on itself: counter only, the other creature assigns by power") {
                val game = castAndBackup(targetOther = false)
                plusOnes(game, game.findPermanent("Streetwise Negotiator")!!) shouldBe 1
                attackWith(game, "Bill the Pony")
                game.getLifeTotal(2) shouldBe 19
            }

            test("printed ability: the 0/2 Negotiator assigns 2 combat damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Streetwise Negotiator")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                attackWith(game, "Streetwise Negotiator")
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
