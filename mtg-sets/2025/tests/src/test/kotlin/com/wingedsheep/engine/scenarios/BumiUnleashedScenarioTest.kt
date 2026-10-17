package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bumi, Unleashed (TLA) — {3}{R}{G} Legendary Creature — Human Noble Ally 5/4.
 *
 *   Trample
 *   When Bumi enters, earthbend 4.
 *   Whenever Bumi deals combat damage to a player, untap all lands you control. After this phase,
 *   there is an additional combat phase. Only land creatures can attack during that combat phase.
 *
 * Exercises the two abilities:
 *  - the ETB earthbend (reused [com.wingedsheep.sdk.dsl.Effects.Earthbend]); and
 *  - the combat-damage trigger, which untaps every land you control and inserts an additional combat
 *    phase in which only *land creatures* may be declared as attackers. This is the reusable
 *    `AddCombatPhaseRestrictedTo` gap: the extra combat phase actually occurs after the current one,
 *    and during it an animated land (a land creature) can attack while a plain creature cannot — yet
 *    that same plain creature attacks freely in the natural combat phase.
 */
class BumiUnleashedScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        test("ETB earthbend 4 turns a target land into a 4/4 creature-land with haste") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Bumi, Unleashed")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bumi, Unleashed").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack() // Bumi enters; the ETB earthbend trigger goes on the stack

            // The earthbend trigger targets a land you control.
            val forest = game.findPermanents("Forest").first()
            game.selectTargets(listOf(forest)).error shouldBe null
            game.resolveStack()

            val projected = projector.project(game.state)
            // The chosen land is now a 0/0 creature-land + four +1/+1 counters = a 4/4 with haste.
            projected.hasType(forest, "LAND") shouldBe true
            projected.hasType(forest, "CREATURE") shouldBe true
            projected.getPower(forest) shouldBe 4
            projected.getToughness(forest) shouldBe 4
            projected.hasKeyword(forest, Keyword.HASTE) shouldBe true
        }

        test("combat damage untaps your lands and grants a land-creatures-only extra combat phase") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Bumi, Unleashed")   // no summoning sickness — can attack
                .withCardOnBattlefield(1, "Centaur Courser")   // a plain (non-land) creature
                .withCardOnBattlefield(1, "Savannah Lions")    // a second plain creature, kept home
                .withCardOnBattlefield(1, "Mountain", tapped = true) // to prove the untap
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInHand(1, "Earthbending Lesson")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Earthbend a Forest into a land creature (with haste) so it is a legal attacker in the
            // upcoming restricted phase. Casting the {3}{G} sorcery also taps Forests for mana.
            val landCreature = game.findPermanents("Forest").first()
            game.castSpell(1, "Earthbending Lesson", landCreature).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val proj0 = projector.project(game.state)
            proj0.hasType(landCreature, "LAND") shouldBe true
            proj0.hasType(landCreature, "CREATURE") shouldBe true

            val bumi = game.findPermanent("Bumi, Unleashed").shouldNotBeNull()
            val centaur = game.findPermanent("Centaur Courser").shouldNotBeNull()
            val lions = game.findPermanent("Savannah Lions").shouldNotBeNull()
            val mountain = game.findPermanent("Mountain").shouldNotBeNull()

            // Natural combat phase: attack with Bumi AND the plain Centaur. Both are legal — proving a
            // non-land creature attacks freely in ordinary combat (the restriction is phase-scoped).
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(
                DeclareAttackers(game.player1Id, mapOf(bumi to game.player2Id, centaur to game.player2Id))
            ).error shouldBe null

            // Resolve blockers + combat damage; Bumi hits Bob, its trigger untaps lands and queues the
            // restricted extra combat phase, which begins as this combat phase ends.
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            // "Untap all lands you control": the land that started tapped is now untapped.
            game.state.getEntity(mountain)?.has<TappedComponent>() shouldBe false

            // An additional combat phase carrying a "land creatures only" attacker restriction is queued.
            val queued = game.state.getEntity(game.player1Id)?.get<AdditionalPhasesComponent>()
            queued.shouldNotBeNull()
            queued.phases.any { it.kind == ExtraPhaseKind.COMBAT && it.attackerRestriction != null } shouldBe true

            // Advance into the inserted extra combat phase.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

            // Savannah Lions is an untapped, non-summoning-sick plain creature — it stayed home, so
            // the ONLY thing stopping it here is the phase restriction: a non-land creature can't
            // attack during this combat phase. (execute leaves state untouched on error.)
            game.execute(DeclareAttackers(game.player1Id, mapOf(lions to game.player2Id)))
                .error shouldNotBe null

            // The earthbended Forest is a land creature (untapped, haste) — it CAN attack here.
            game.execute(DeclareAttackers(game.player1Id, mapOf(landCreature to game.player2Id)))
                .error shouldBe null
        }
    }
}
