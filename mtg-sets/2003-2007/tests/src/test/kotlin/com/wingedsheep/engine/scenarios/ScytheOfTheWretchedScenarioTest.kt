package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scythe of the Wretched (MRD #239).
 *
 * {2} Artifact — Equipment
 * "Equipped creature gets +2/+2.
 *  Whenever a creature dealt damage by equipped creature this turn dies, return that card to the
 *  battlefield under your control. Attach this Equipment to that creature.
 *  Equip {4}"
 *
 * The trigger is the Soul Collector shape read one object further out: the engine already tracked
 * "creatures this permanent damaged this turn" on the damaging source, and `TriggerBinding.ATTACHED`
 * now means the damaging source is the permanent this is attached to. So the tests are all about
 * *which* creature's damage counts:
 *  - the equipped creature's damage fires it,
 *  - an unattached Scythe never fires (no attachment target to read the tracker off),
 *  - and another creature you control killing something does *not* fire it — the tracker is read off
 *    the attachment target, not off any creature of yours.
 *
 * The payoff half is worth its own assertions because it moves the creature *and* the Equipment:
 * the card comes back under the Scythe's controller (not its owner's), and the Scythe leaves its old
 * host to attach to it.
 */
class ScytheOfTheWretchedScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Scythe of the Wretched") {

            test("equipped creature gets +2/+2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Scythe of the Wretched", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val projected = stateProjector.project(game.state)
                withClue("a 3/3 equipped with the Scythe is a 5/5") {
                    projected.getPower(giant) shouldBe 5
                    projected.getToughness(giant) shouldBe 5
                }
            }

            test("a creature the equipped creature killed returns under your control, wearing the Scythe") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")          // 3/3, becomes 5/5
                    .withCardAttachedTo(1, "Scythe of the Wretched", "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")       // 2/2 blocker, theirs
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scythe = game.findPermanent("Scythe of the Wretched")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("the Bears died to combat damage and came back onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("'under your control' — the Scythe's controller, not the card's owner") {
                    game.controllerOf(bears) shouldBe game.player1Id
                }
                withClue("the Scythe moved onto the returned creature, leaving its old host") {
                    game.attachmentOf(scythe) shouldBe bears
                }
                withClue("the 5/5 attacker survived the 2 damage it took") {
                    game.findPermanent("Hill Giant") shouldBe giant
                }
                withClue("+2/+2 follows the Equipment: the 2/2 it just attached to is now a 4/4") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                }
                withClue("and the old host is back to its printed 3/3") {
                    stateProjector.project(game.state).getPower(giant) shouldBe 3
                }
            }

            test("an unattached Scythe never fires") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Scythe of the Wretched")   // never attached
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("with no attachment target there is no damaging source to read, so nothing returns") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }

            test("a different creature you control killing something does not fire it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")            // the killer, unequipped
                    .withCardOnBattlefield(1, "Wind Drake")            // the Scythe's host, stays home
                    .withCardAttachedTo(1, "Scythe of the Wretched", "Wind Drake")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("'equipped creature' is the attachment target, not any creature you control") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }
        }
    }
}

/** The player currently controlling [id]. */
private fun ScenarioTestBase.TestGame.controllerOf(id: EntityId): EntityId? =
    state.getEntity(id)?.get<ControllerComponent>()?.playerId

/** What [id] is attached to, or null when it is unattached. */
private fun ScenarioTestBase.TestGame.attachmentOf(id: EntityId): EntityId? =
    state.getEntity(id)?.get<AttachedToComponent>()?.targetId
