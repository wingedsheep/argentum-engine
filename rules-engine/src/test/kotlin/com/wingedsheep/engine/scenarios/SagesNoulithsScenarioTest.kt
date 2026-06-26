package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sage's Nouliths (FIN) — {1}{U} Artifact — Equipment (job select).
 *
 *  "Job select (When this Equipment enters, create a 1/1 colorless Hero creature token, then attach this to it.)
 *   Equipped creature gets +1/+0, has 'Whenever this creature attacks, untap target attacking creature,'
 *   and is a Cleric in addition to its other types.
 *   Hagneia — Equip {3}"
 *
 * The job-select ETB shell (Hero token + auto-attach) is proven by JobSelectScenarioTest; this test
 * focuses on the equipped-creature bonus (+1/+0, Cleric) and the granted attack trigger that untaps a
 * target attacking creature.
 */
class SagesNoulithsScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("Sage's Nouliths: +1/+0, Cleric, and the granted attack trigger untaps a target attacking creature") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Sage's Nouliths")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sage's Nouliths").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val hero = game.findPermanent("Hero Token")!!
            val projected = stateProjector.project(game.state)
            withClue("Equipped Hero token should be 2/1 (1/1 base + 1/+0)") {
                projected.getPower(hero) shouldBe 2
                projected.getToughness(hero) shouldBe 1
            }
            withClue("Equipped Hero token should be a Cleric in addition to its other types") {
                projected.hasSubtype(hero, "Cleric") shouldBe true
            }

            // Attack with the equipped Hero token: it taps, then the granted trigger fires and
            // (with the Hero as the only attacking creature) untaps that same attacker.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hero Token" to 2))
            withClue("Hero is tapped right after being declared as an attacker") {
                game.state.getEntity(hero)?.has<TappedComponent>() shouldBe true
            }
            if (game.hasPendingDecision()) game.selectTargets(listOf(hero))
            game.passPriority()
            game.resolveStack()

            withClue("The granted trigger untapped the attacking equipped creature") {
                game.state.getEntity(hero)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
