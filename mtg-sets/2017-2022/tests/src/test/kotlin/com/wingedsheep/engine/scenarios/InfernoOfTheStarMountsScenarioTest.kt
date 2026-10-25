package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.InfernoOfTheStarMounts
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Inferno of the Star Mounts — "{R}: gets +1/+0 until end of turn. When its power becomes 20 this
 * way, it deals 20 damage to any target." The reflexive trigger fires only when the pump itself
 * takes its power to exactly 20 (rulings 2021-07-23): not when another effect skips it past 20,
 * and its target is chosen only once it triggers, not at activation.
 */
class InfernoOfTheStarMountsScenarioTest : ScenarioTestBase() {

    init {
        val pumpId = InfernoOfTheStarMounts.activatedAbilities.single().id

        fun TestGame.pumpAndResolve() {
            val inferno = findPermanent("Inferno of the Star Mounts")!!
            execute(ActivateAbility(playerId = player1Id, sourceId = inferno, abilityId = pumpId))
                .error.shouldBeNull()
            resolveStack()
        }

        fun TestGame.power(): Int =
            state.projectedState.getPower(findPermanent("Inferno of the Star Mounts")!!)!!

        test("the pump that takes its power from 19 to 20 triggers 20 damage to a target chosen then") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Inferno of the Star Mounts")
                .withLandsOnBattlefield(1, "Mountain", 15)
                .withLifeTotal(2, 30)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            repeat(13) {
                game.pumpAndResolve()
                game.getPendingDecision().shouldBeNull()
            }
            game.power() shouldBe 19

            game.pumpAndResolve()
            game.power() shouldBe 20
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error.shouldBeNull()
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 10

            // 20 -> 21 through the same ability doesn't trigger again.
            game.pumpAndResolve()
            game.power() shouldBe 21
            game.getPendingDecision().shouldBeNull()
            game.getLifeTotal(2) shouldBe 10
        }

        test("skipping past 20 with another effect never triggers") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Inferno of the Star Mounts")
                .withLandsOnBattlefield(1, "Mountain", 14)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(1, "Giant Growth")
                .withLifeTotal(2, 30)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            repeat(13) { game.pumpAndResolve() }
            game.power() shouldBe 19

            // Giant Growth takes it 19 -> 22: "becomes 20" in some other way doesn't count.
            game.castSpell(1, "Giant Growth", game.findPermanent("Inferno of the Star Mounts")!!)
                .error.shouldBeNull()
            game.resolveStack()
            game.power() shouldBe 22
            game.getPendingDecision().shouldBeNull()

            game.pumpAndResolve()
            game.power() shouldBe 23
            game.getPendingDecision().shouldBeNull()
            game.getLifeTotal(2) shouldBe 30
        }
    }
}
