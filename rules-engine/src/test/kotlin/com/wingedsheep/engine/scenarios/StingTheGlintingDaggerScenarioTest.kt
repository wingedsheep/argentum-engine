package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sting, the Glinting Dagger (LTR) —
 *   "Equipped creature gets +1/+1 and has haste.
 *    At the beginning of each combat, untap equipped creature.
 *    Equipped creature has first strike as long as it's blocking or blocked by a Goblin or Orc.
 *    Equip {2}"
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.conditions.SourceIsBlockingOrBlockedBySubtype]
 * condition wrapping a conditional first-strike grant on the equipped creature, plus the composed
 * +1/+1, haste, and begin-of-combat untap pieces.
 */
class StingTheGlintingDaggerScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Sting, the Glinting Dagger").activatedAbilities
            .single { it.isEquipAbility }.id
    }

    init {
        test("equip grants +1/+1 and haste") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = true) // 3/3
                .withCardOnBattlefield(1, "Sting, the Glinting Dagger")
                .withLandsOnBattlefield(1, "Plains", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            val sting = game.findPermanent("Sting, the Glinting Dagger")!!

            game.state.projectedState.hasKeyword(courser, Keyword.HASTE) shouldBe false

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sting,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(courser))
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(courser) shouldBe 4
            game.state.projectedState.getToughness(courser) shouldBe 4
            game.state.projectedState.hasKeyword(courser, Keyword.HASTE) shouldBe true
        }

        test("beginning of each combat untaps the equipped creature") {
            val game = scenario()
                .withPlayers()
                // Equipped creature starts tapped; the begin-combat trigger should untap it.
                .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
                .withCardOnBattlefield(1, "Sting, the Glinting Dagger")
                .withLandsOnBattlefield(1, "Plains", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            val sting = game.findPermanent("Sting, the Glinting Dagger")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sting,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(courser))
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(courser)?.has<TappedComponent>() shouldBe true

            // Advance into combat; the begin-combat trigger fires and untaps the equipped creature.
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.state.getEntity(courser)?.has<TappedComponent>() shouldBe false
        }

        test("equipped creature has first strike when blocked by a Goblin") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Centaur Courser") // 3/3 attacker (4/4 equipped)
                .withCardOnBattlefield(1, "Sting, the Glinting Dagger")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Goblin Guide") // 2/1 Goblin blocker
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            val sting = game.findPermanent("Sting, the Glinting Dagger")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sting,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(courser))
                )
            ).error shouldBe null
            game.resolveStack()

            // No combat yet — no first strike.
            game.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Goblin Guide" to listOf("Centaur Courser"))).error shouldBe null

            // Now blocked by a Goblin — the conditional grant turns on first strike.
            game.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe true
        }

        test("equipped creature does NOT have first strike when blocked by a non-Goblin/Orc") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Centaur Courser") // 3/3 attacker (4/4 equipped)
                .withCardOnBattlefield(1, "Sting, the Glinting Dagger")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Savannah Lions") // 1/1 Cat blocker
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            val sting = game.findPermanent("Sting, the Glinting Dagger")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sting,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(courser))
                )
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Savannah Lions" to listOf("Centaur Courser"))).error shouldBe null

            // Blocked by a Cat — the condition is not met, so no first strike.
            game.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false
        }
    }
}
