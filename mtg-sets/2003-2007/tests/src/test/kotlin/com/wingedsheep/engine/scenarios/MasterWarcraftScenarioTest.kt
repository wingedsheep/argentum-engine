package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.mechanics.combat.CombatDeclarationControl
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Master Warcraft (RAV #250) — "Cast this spell only before attackers are declared. You choose
 * which creatures attack this turn. You choose which creatures block this turn and how those
 * creatures block."
 *
 * The declarations move to the caster and nothing else does: the legal declare-attackers /
 * declare-blockers action is offered to the caster (and withheld from the seat that owes it), the
 * declaration itself is still that seat's and is validated against its creatures, and the effect
 * lapses with the turn. The timing line allows casting up to the beginning of combat step of the
 * first combat, and not from the declare attackers step on.
 */
class MasterWarcraftScenarioTest : ScenarioTestBase() {

    private fun board(caster: Int, phase: Phase = Phase.PRECOMBAT_MAIN, step: Step = Step.PRECOMBAT_MAIN): TestGame =
        scenario()
            .withPlayers("Active", "Defender")
            .withCardInHand(caster, "Master Warcraft")
            .withLandsOnBattlefield(caster, "Mountain", 4)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Llanowar Elves")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(phase, step)
            .build()

    private fun TestGame.advanceTo(step: Step) {
        var guard = 0
        while (state.step != step && guard++ < 20) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
        }
        state.step shouldBe step
    }

    init {
        test("cast on your own turn: you declare the defending player's blocks") {
            val game = board(caster = 1)
            game.castSpell(1, "Master Warcraft").error shouldBe null
            game.resolveStack()
            CombatDeclarationControl.controllerOf(game.state, CombatDeclaration.BLOCKERS, game.player2Id) shouldBe game.player1Id

            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

            game.advanceTo(Step.DECLARE_BLOCKERS)
            withClue("the defender owes the block declaration but no longer makes it") {
                CombatDeclarationControl.declarerFor(game.state, game.player2Id) shouldBe game.player1Id
                game.getLegalActions(2).none { it.action is DeclareBlockers } shouldBe true
            }
            val offer = game.getLegalActions(1).single { it.action is DeclareBlockers }
            withClue("the caster is offered the defender's declaration, over the defender's creatures") {
                (offer.action as DeclareBlockers).playerId shouldBe game.player2Id
                offer.validBlockers!! shouldContain game.findPermanent("Llanowar Elves")!!
            }

            // The caster makes the Elves chump-block the Bears and keeps the Giant home.
            val elves = game.findPermanent("Llanowar Elves")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(DeclareBlockers(game.player2Id, mapOf(elves to listOf(bears)))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the Elves blocked as the caster chose and died") {
                game.findPermanent("Llanowar Elves") shouldBe null
                game.findPermanent("Hill Giant") shouldNotBe null
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("cast by the defender: they choose the active player's attackers") {
            val game = board(caster = 2)
            game.execute(PassPriority(game.player1Id)).error shouldBe null
            game.castSpell(2, "Master Warcraft").error shouldBe null
            game.resolveStack()
            CombatDeclarationControl.controllerOf(game.state, CombatDeclaration.ATTACKERS, game.player1Id) shouldBe game.player2Id

            game.advanceTo(Step.DECLARE_ATTACKERS)
            withClue("the active player doesn't get to declare attackers") {
                game.getLegalActions(1).none { it.action is DeclareAttackers } shouldBe true
            }
            val offer = game.getLegalActions(2).single { it.action is DeclareAttackers }
            (offer.action as DeclareAttackers).playerId shouldBe game.player1Id

            // The defender sends only the Lions in, straight into the Hill Giant.
            val lions = game.findPermanent("Savannah Lions")!!
            game.execute(DeclareAttackers(game.player1Id, mapOf(lions to game.player2Id))).error shouldBe null
            withClue("the attackers are still the active player's creatures") {
                game.state.getEntity(lions)!!.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() shouldBe true
            }
            game.advanceTo(Step.DECLARE_BLOCKERS)
            withClue("the defender blocks for themselves as usual — they are the declarer anyway") {
                game.getLegalActions(2).any { it.action is DeclareBlockers } shouldBe true
            }
        }

        test("an illegal declaration is still refused for its real controller") {
            val game = board(caster = 2)
            game.execute(PassPriority(game.player1Id)).error shouldBe null
            game.castSpell(2, "Master Warcraft").error shouldBe null
            game.resolveStack()
            game.advanceTo(Step.DECLARE_ATTACKERS)
            withClue("the defender can't make the active player attack with the defender's own creature") {
                val giant = game.findPermanent("Hill Giant")!!
                game.execute(DeclareAttackers(game.player1Id, mapOf(giant to game.player2Id))).error shouldNotBe null
            }
        }

        test("the effect lasts only this turn") {
            val game = board(caster = 1)
            game.castSpell(1, "Master Warcraft").error shouldBe null
            game.resolveStack()
            game.state = game.state.copy(turnNumber = game.state.turnNumber + 1)
            CombatDeclarationControl.controllerOf(game.state, CombatDeclaration.BLOCKERS, game.player2Id) shouldBe null
        }

        test("castable up to the beginning of combat, not once attackers are being declared") {
            board(caster = 1, phase = Phase.COMBAT, step = Step.BEGIN_COMBAT)
                .castSpell(1, "Master Warcraft").error shouldBe null
            board(caster = 1, phase = Phase.COMBAT, step = Step.DECLARE_ATTACKERS)
                .castSpell(1, "Master Warcraft").error shouldNotBe null
            board(caster = 1, phase = Phase.POSTCOMBAT_MAIN, step = Step.POSTCOMBAT_MAIN)
                .castSpell(1, "Master Warcraft").error shouldNotBe null
        }
    }
}
