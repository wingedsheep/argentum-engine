package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.PlagueNurse
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Plague Nurse (ONE #179) — {3}{G} 3/4 Creature — Phyrexian Cleric
 *   Toxic 2
 *   {2}{G}: Each other creature you control with toxic gains toxic 1 until end of turn.
 *   Activate only once each turn.
 */
class PlagueNurseScenarioTest : ScenarioTestBase() {

    private fun TestGame.poison(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    init {
        test("other toxic creatures gain toxic 1; the Nurse and non-toxic creatures do not; once per turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Plague Nurse")
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val nurse = game.findPermanent("Plague Nurse")!!
            val abilityId = PlagueNurse.activatedAbilities.single().id

            game.execute(ActivateAbility(game.player1Id, nurse, abilityId)).error shouldBe null
            game.resolveStack()

            // Activate only once each turn.
            game.execute(ActivateAbility(game.player1Id, nurse, abilityId)).error shouldNotBe null

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Plague Nurse" to 2, "Blightbelly Rat" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Nurse: toxic 2 (not "other"). Rat: toxic 1 + 1 = 2. Bears: no toxic, so none granted.
            game.poison(game.player2Id) shouldBe 4
        }

        test("granted toxic wears off at end of turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Plague Nurse")
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val nurse = game.findPermanent("Plague Nurse")!!
            val rat = game.findPermanent("Blightbelly Rat")!!
            val abilityId = PlagueNurse.activatedAbilities.single().id

            val before = game.state.projectedState.getKeywords(rat)
            game.execute(ActivateAbility(game.player1Id, nurse, abilityId)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(rat, "TOXIC_2") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.projectedState.getKeywords(rat) shouldBe before
        }
    }
}
