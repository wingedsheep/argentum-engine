package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Trailblazer's Boots — "Equipped creature has nonbasic landwalk." Exercises the new
 * Keyword.NONBASIC_LANDWALK evasion rule: the equipped attacker can't be blocked while the
 * defending player controls a nonbasic land, but can be blocked when they control only basics.
 */
class TrailblazersBootsScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Trailblazer's Boots").activatedAbilities[0].id
    }

    private fun equippedAttackerGame(defenderLand: String) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Centaur Courser")     // 2/3 attacker
        .withCardOnBattlefield(1, "Trailblazer's Boots")
        .withCardOnBattlefield(1, "Forest")
        .withCardOnBattlefield(1, "Forest")
        .withCardOnBattlefield(2, "Grizzly Bears")        // would-be blocker
        .withCardOnBattlefield(2, defenderLand)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("equipped creature can't be blocked while defender controls a nonbasic land") {
            val game = equippedAttackerGame(defenderLand = "Mines of Moria")
            val courser = game.findPermanent("Centaur Courser")!!
            val boots = game.findPermanent("Trailblazer's Boots")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = boots,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(courser))
                )
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            // Defender controls a nonbasic land (Mines of Moria) → the block is illegal.
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser")))
                .error.shouldNotBeNull()
        }

        test("equipped creature can be blocked when defender controls only basic lands") {
            val game = equippedAttackerGame(defenderLand = "Forest")
            val courser = game.findPermanent("Centaur Courser")!!
            val boots = game.findPermanent("Trailblazer's Boots")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = boots,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(courser))
                )
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            // Defender controls only a basic Forest → the block is legal.
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser")))
                .error.shouldBeNull()
        }
    }
}
