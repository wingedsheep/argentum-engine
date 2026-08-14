package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stitcher's Graft — "Equipped creature gets +3/+3. Whenever equipped creature attacks, it doesn't
 * untap during its controller's next untap step. Whenever this Equipment becomes unattached from a
 * permanent, sacrifice that permanent. Equip {2}."
 *
 * The card exists to exercise [com.wingedsheep.sdk.dsl.Triggers.becomesUnattached], so the tests
 * walk the unattach paths the 2016-07-13 ruling enumerates: re-equipping elsewhere, the Equipment
 * leaving the battlefield, and the host leaving the battlefield (where the trigger fires but has
 * nothing left to sacrifice). The CR 704.5n state-based unattach shares the same chokepoint.
 */
class StitchersGraftScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Stitcher's Graft").activatedAbilities[0].id
    }

    /** Board: player 1 has the Graft plus two vanilla creatures and enough lands to equip twice. */
    private fun board() = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(1, "Stitcher's Graft")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("equipped creature gets +3/+3") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val graft = game.findPermanent("Stitcher's Graft")!!

            game.execute(
                ActivateAbility(game.player1Id, graft, equipAbilityId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 5
        }

        test("attacking with the equipped creature keeps it tapped through its controller's next untap step") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val graft = game.findPermanent("Stitcher's Graft")!!

            game.execute(
                ActivateAbility(game.player1Id, graft, equipAbilityId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            // Put the "doesn't untap" trigger on the stack and resolve it.
            game.passPriority()
            game.resolveStack()

            withClue("attacking taps the Bears") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }

            // Roll forward until player 1 is the active player again in their main phase — that is
            // past player 1's next untap step, the one the grant eats.
            var guard = 0
            while (!(game.state.activePlayerId == game.player1Id &&
                    game.state.step == Step.PRECOMBAT_MAIN) && guard++ < 20
            ) {
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (game.state.activePlayerId == game.player1Id) break
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            }

            withClue(
                "the Bears skipped their controller's next untap step (turn ${game.state.turnNumber}, " +
                    "active = ${game.state.activePlayerId})"
            ) {
                game.state.activePlayerId shouldBe game.player1Id
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }

        test("equipping the Graft onto another creature sacrifices the creature it came off") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val graft = game.findPermanent("Stitcher's Graft")!!

            game.execute(
                ActivateAbility(game.player1Id, graft, equipAbilityId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(graft)?.get<AttachedToComponent>()?.targetId shouldBe bears

            // Move the Graft to the Hill Giant — the Bears become unattached from it.
            game.execute(
                ActivateAbility(game.player1Id, graft, equipAbilityId, listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            game.resolveStack()

            withClue("the Graft moved onto the Hill Giant") {
                game.state.getEntity(graft)?.get<AttachedToComponent>()?.targetId shouldBe giant
            }
            withClue("the Bears were sacrificed when the Graft came off them") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
            }
            withClue("the Hill Giant is unharmed and now carries the +3/+3") {
                game.state.projectedState.getPower(giant) shouldBe 6
            }
        }

        test("destroying the Graft sacrifices the creature it was equipping") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Stitcher's Graft")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInHand(2, "Naturalize")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val graft = game.findPermanent("Stitcher's Graft")!!

            game.execute(
                ActivateAbility(game.player1Id, graft, equipAbilityId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            // Opponent blows up the Graft. It becomes unattached because it left the battlefield,
            // so its own last trigger still fires (CR 603.6e) and eats the Bears.
            game.passPriority()
            game.castSpell(2, "Naturalize", targetId = graft).error shouldBe null
            game.resolveStack()

            withClue("the Graft is in the graveyard") {
                game.findPermanent("Stitcher's Graft") shouldBe null
            }
            withClue("the Bears were sacrificed by the Graft's unattach trigger") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
            }
        }

        test("the equipped creature leaving the battlefield fires the trigger but sacrifices nothing") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Stitcher's Graft")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInHand(2, "Murder")
                .withLandsOnBattlefield(2, "Swamp", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Swamp")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val graft = game.findPermanent("Stitcher's Graft")!!

            game.execute(
                ActivateAbility(game.player1Id, graft, equipAbilityId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.passPriority()
            game.castSpell(2, "Murder", targetId = bears).error shouldBe null
            game.resolveStack()

            withClue("the Bears died to the removal spell") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
            withClue("the Graft unattached (CR 704.5n) and stayed on the battlefield") {
                game.findPermanent("Stitcher's Graft") shouldBe graft
                game.state.getEntity(graft)?.has<AttachedToComponent>() shouldBe false
            }
            withClue("the trigger had nothing to sacrifice — the untouched Hill Giant survives") {
                game.findPermanent("Hill Giant") shouldBe giant
            }
        }
    }
}
