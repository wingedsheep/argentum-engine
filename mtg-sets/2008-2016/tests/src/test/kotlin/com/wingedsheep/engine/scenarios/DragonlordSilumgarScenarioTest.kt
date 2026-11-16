package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dragonlord Silumgar (DTK #220) — {4}{U}{B} 3/5 Legendary Elder Dragon, flying, deathtouch.
 *
 * "When Dragonlord Silumgar enters, gain control of target creature or planeswalker for as long as
 * you control Dragonlord Silumgar."
 *
 * Pins the steal (creatures and planeswalkers are targetable), its end when Silumgar leaves, and
 * the ruling that a Silumgar gone before its trigger resolves steals nothing at all.
 */
class DragonlordSilumgarScenarioTest : ScenarioTestBase() {

    init {
        context("Dragonlord Silumgar") {

            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Dragonlord Silumgar")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(2, "Sorin, Solemn Visitor")
                .withCardInHand(2, "Murder")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("steals target creature, and control returns when Silumgar leaves the battlefield") {
                val game = board()
                val courser = game.findPermanent("Centaur Courser")!!
                val sorin = game.findPermanent("Sorin, Solemn Visitor")!!

                game.castSpell(1, "Dragonlord Silumgar").error shouldBe null
                game.resolveStack()
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets[0]!! shouldContainAll listOf(courser, sorin)
                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getController(courser) shouldBe game.player1Id

                val silumgar = game.findPermanent("Dragonlord Silumgar")!!
                game.passPriority() // Alice passes with an empty stack; Bob gets priority
                game.castSpell(2, "Murder", silumgar).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Dragonlord Silumgar") shouldBe true
                game.state.projectedState.getController(courser) shouldBe game.player2Id
            }

            test("can steal a planeswalker") {
                val game = board()
                val sorin = game.findPermanent("Sorin, Solemn Visitor")!!

                game.castSpell(1, "Dragonlord Silumgar").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(sorin)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getController(sorin) shouldBe game.player1Id
            }

            test("Silumgar killed in response to its trigger steals nothing") {
                val game = board()
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Dragonlord Silumgar").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(courser)).error shouldBe null
                val silumgar = game.findPermanent("Dragonlord Silumgar")!!

                game.execute(PassPriority(game.player1Id)).error shouldBe null
                game.castSpell(2, "Murder", silumgar).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Dragonlord Silumgar") shouldBe true
                game.state.stack.isEmpty() shouldBe true
                game.state.projectedState.getController(courser) shouldBe game.player2Id
                game.state.floatingEffects.none { courser in it.effect.affectedEntities } shouldBe true
            }
        }
    }
}
