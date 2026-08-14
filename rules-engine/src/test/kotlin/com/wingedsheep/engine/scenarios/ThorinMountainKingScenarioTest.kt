package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thorin, Mountain-king (HOB #114) — {3}{R} Legendary Creature — Dwarf Noble 3/4, trample.
 *
 * "When Thorin enters, attach any number of target Equipment you control to target creature you
 * control. When one or more Equipment become attached to that creature this way, that creature
 * deals damage equal to its power to up to one target creature."
 *
 * The interesting parts are all composition seams, so each gets a test:
 *  - two target requirements with the unbounded Equipment slot declared *last*, and the creature
 *    slot therefore stable at index 0;
 *  - the attach loop, which must re-attach Equipment already sitting on another creature;
 *  - the CR 603.12 reflexive trigger, whose damage is dealt by the *equipped creature* for *its*
 *    post-attach power — proving the equipped-creature collection survives onto the reflexive;
 *  - the "one or more Equipment become attached" guard: choosing zero Equipment must not fire the
 *    damage half at all.
 */
class ThorinMountainKingScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("attaches both target Equipment, then the equipped creature deals its boosted power") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Thorin, Mountain-king")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Buster Sword") // +3/+2
                .withCardOnBattlefield(1, "Buster Sword") // +3/+2
                .withCardOnBattlefield(2, "Thing from the Deep") // 9/9, survives 8 damage
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val swords = game.findPermanents("Buster Sword")
            val leviathan = game.findPermanent("Thing from the Deep")!!
            swords.size shouldBe 2

            game.castSpell(1, "Thorin, Mountain-king").error shouldBe null
            game.resolveStack()

            // The ETB trigger targets: slot 0 = the creature, slot 1 = any number of Equipment.
            val etbTargets = game.getPendingDecision()
            withClue("the enters trigger asks for its targets") { etbTargets shouldBe game.getPendingDecision() }
            game.submitDecision(
                TargetsResponse(etbTargets!!.id, mapOf(0 to listOf(bears), 1 to swords))
            )
            game.resolveStack()

            withClue("both Equipment attached to the target creature") {
                swords.forEach { sword ->
                    game.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                val projected = stateProjector.project(game.state)
                projected.getPower(bears) shouldBe 8
                projected.getToughness(bears) shouldBe 6
            }

            // The reflexive "when one or more Equipment become attached" trigger picks its own target.
            val reflexiveTargets = game.getPendingDecision()
            withClue("the reflexive trigger asks for its own 'up to one target creature'") {
                (reflexiveTargets != null) shouldBe true
            }
            game.submitDecision(
                TargetsResponse(reflexiveTargets!!.id, mapOf(0 to listOf(leviathan)))
            )
            game.resolveStack()

            withClue("the equipped creature deals damage equal to its post-attach power (2 + 3 + 3)") {
                game.state.getEntity(leviathan)?.get<DamageComponent>()?.amount shouldBe 8
            }
        }

        test("re-attaches an Equipment already equipping something else") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Thorin, Mountain-king")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Buster Sword")
                .withCardAttachedTo(1, "Buster Sword", "Hill Giant")
                .withCardOnBattlefield(2, "Thing from the Deep")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val sword = game.findPermanent("Buster Sword")!!
            val leviathan = game.findPermanent("Thing from the Deep")!!

            game.castSpell(1, "Thorin, Mountain-king").error shouldBe null
            game.resolveStack()

            val etbTargets = game.getPendingDecision()!!
            game.submitDecision(
                TargetsResponse(etbTargets.id, mapOf(0 to listOf(bears), 1 to listOf(sword)))
            )
            game.resolveStack()

            withClue("the Equipment moved off Hill Giant and onto Grizzly Bears") {
                game.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe bears
                val projected = stateProjector.project(game.state)
                projected.getPower(bears) shouldBe 5
                projected.getPower(giant) shouldBe 3
            }

            val reflexiveTargets = game.getPendingDecision()!!
            game.submitDecision(
                TargetsResponse(reflexiveTargets.id, mapOf(0 to listOf(leviathan)))
            )
            game.resolveStack()

            withClue("damage is the equipped creature's power after the move (2 + 3)") {
                game.state.getEntity(leviathan)?.get<DamageComponent>()?.amount shouldBe 5
            }
        }

        test("choosing zero Equipment attaches nothing and never fires the damage trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Thorin, Mountain-king")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Buster Sword")
                .withCardOnBattlefield(2, "Thing from the Deep")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val sword = game.findPermanent("Buster Sword")!!
            val leviathan = game.findPermanent("Thing from the Deep")!!

            game.castSpell(1, "Thorin, Mountain-king").error shouldBe null
            game.resolveStack()

            // "Any number of target Equipment" allows none — submit only the creature slot.
            val etbTargets = game.getPendingDecision()!!
            game.submitDecision(
                TargetsResponse(etbTargets.id, mapOf(0 to listOf(bears), 1 to emptyList()))
            )
            game.resolveStack()

            withClue("nothing attached, and no reflexive damage trigger was created") {
                game.state.getEntity(sword)?.get<AttachedToComponent>() shouldBe null
                stateProjector.project(game.state).getPower(bears) shouldBe 2
                game.hasPendingDecision() shouldBe false
                game.state.getEntity(leviathan)?.get<DamageComponent>()?.amount shouldBe null
            }
        }
    }
}
