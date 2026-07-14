package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.CavernStomper
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Cavern Stomper (LCI #177) — {4}{G}{G} Creature — Dinosaur, 7/7, Common.
 *
 * "When this creature enters, scry 2.
 *  {3}{G}: This creature can't be blocked by creatures with power 2 or less this turn."
 *
 * Focus: the activated evasion ability grants a [com.wingedsheep.sdk.scripting.CantBeBlockedBy]
 * static ability, which lives in `GameState.grantedStaticAbilities` (combat reads it directly, not
 * through the layer system) and so never reaches the projected keyword set that feeds `abilityFlags`.
 * This pins that the grant is surfaced to the client as a "Granted Ability" badge — without it the
 * player has no on-creature indicator that the restriction is active.
 */
class CavernStomperScenarioTest : ScenarioTestBase() {

    init {
        // Cavern Stomper is auto-discovered from the LCI cards package (already in the shared
        // cardRegistry). No explicit registration needed.
        val activateAbilityId = CavernStomper.activatedAbilities.first().id

        context("Cavern Stomper") {

            test("{3}{G} evasion grant is surfaced to the client as a Granted Ability badge") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern Stomper")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stomper = game.findPermanent("Cavern Stomper")!!

                // Before activation there is no granted-ability badge on the creature.
                withClue("no granted-ability badge before the ability is activated") {
                    game.getClientState(1).cards.getValue(stomper)
                        .activeEffects.none { it.icon == "granted-ability" } shouldBe true
                }

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = stomper, abilityId = activateAbilityId)
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                // The granted restriction is now surfaced as a single badge carrying its description.
                val badges = game.getClientState(1).cards.getValue(stomper)
                    .activeEffects.filter { it.icon == "granted-ability" }
                withClue("exactly one granted-ability badge") { badges.size shouldBe 1 }
                withClue("badge description names the block restriction: ${badges.firstOrNull()?.description}") {
                    (badges.single().description?.contains("power 2 or less") == true) shouldBe true
                }
            }
        }
    }
}
