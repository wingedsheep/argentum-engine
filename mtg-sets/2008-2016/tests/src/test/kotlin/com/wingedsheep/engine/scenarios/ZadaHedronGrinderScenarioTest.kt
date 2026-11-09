package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Zada, Hedron Grinder — "Whenever you cast an instant or sorcery spell that targets only Zada, copy
 * that spell for each other creature you control that the spell could target. Each copy targets a
 * different one of those creatures."
 *
 * Covers one copy per eligible creature, the multi-target rulings (every target on Zada triggers,
 * any other target doesn't), spells aimed elsewhere, and the "you cast" scope.
 */
class ZadaHedronGrinderScenarioTest : ScenarioTestBase() {

    /** Seeds of Strength has three separate "target creature" instances. */
    private fun TestGame.castSeeds(targets: List<EntityId>): ExecutionResult {
        val seeds = state.getHand(player1Id).first { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == "Seeds of Strength"
        }
        return execute(CastSpell(player1Id, seeds, targets.map { ChosenTarget.Permanent(it) }))
    }

    init {
        test("copies the spell once for each other creature you control") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Zada, Hedron Grinder")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zada = game.findPermanent("Zada, Hedron Grinder")!!
            game.castSpell(1, "Giant Growth", zada).error shouldBe null

            // Resolve only the trigger: the original plus exactly two copies wait on the stack.
            game.passPriority()
            game.passPriority()
            game.state.stack.count { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Giant Growth"
            } shouldBe 3

            game.resolveStack()
            val projected = game.state.projectedState
            projected.getPower(zada) shouldBe 6
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 5
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 6
            // The opponent's creature is not "a creature you control".
            projected.getPower(game.findPermanent("Raging Goblin")!!) shouldBe 1
        }

        test("a multi-target spell aimed at Zada with every target copies onto each other creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Zada, Hedron Grinder")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Seeds of Strength")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zada = game.findPermanent("Zada, Hedron Grinder")!!
            game.castSeeds(listOf(zada, zada, zada)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(zada) shouldBe 6
            // The copy's every target is the Bears, so it gets all three +1/+1s.
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 5
        }

        test("does not trigger when the spell also targets another creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Zada, Hedron Grinder")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Seeds of Strength")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zada = game.findPermanent("Zada, Hedron Grinder")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSeeds(listOf(zada, zada, bears)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(zada) shouldBe 5
            projected.getPower(bears) shouldBe 3
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
        }

        test("does not trigger on a spell that targets a creature other than Zada") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Zada, Hedron Grinder")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 5
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
            projected.getPower(game.findPermanent("Zada, Hedron Grinder")!!) shouldBe 3
        }

        test("does not trigger when an opponent casts the spell at Zada") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Zada, Hedron Grinder")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zada = game.findPermanent("Zada, Hedron Grinder")!!
            game.castSpell(1, "Giant Growth", zada).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(zada) shouldBe 6
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }
    }
}
