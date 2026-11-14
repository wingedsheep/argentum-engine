package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisCombatComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.QueuedPhase
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `TurnTracker.CREATURES_ATTACKED_THIS_COMBAT` — the creatures declared as attackers in the current
 * combat (CR 508.1), counted even after they leave the battlefield, never counting a creature put
 * onto the battlefield attacking, and reset when the combat phase ends.
 */
class CreaturesAttackedThisCombatTest : FunSpec({
    fun setup(players: Int = 2, teamGame: Boolean = false): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        if (players == 2 && !teamGame) {
            initMirrorMatch(Deck.of("Forest" to 40))
        } else {
            initMultiplayer(
                List(players) { Deck.of("Forest" to 40) },
                format = if (teamGame) Format.TwoHeadedGiant() else Format.Standard,
                teams = if (teamGame) listOf(listOf(0, 1), listOf(2, 3)) else null,
            )
        }
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val evaluator = PredicateEvaluator(cardRegistry = null)
    fun count(d: GameTestDriver, player: Player = Player.Each, viewer: EntityId = d.player1): Int =
        evaluator.amounts.evaluate(d.state, DynamicAmounts.creaturesAttackedThisCombat(player), EffectContext(null, viewer))
    fun attacker(d: GameTestDriver, owner: EntityId = d.player1): EntityId =
        d.putCreatureOnBattlefield(owner, "Grizzly Bears").also { d.removeSummoningSickness(it) }
    fun attack(d: GameTestDriver, assignments: Map<EntityId, EntityId>, by: EntityId = d.player1) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(by, assignments).error shouldBe null
    }

    test("counts every declared attacker, credited to the attacking player") {
        val d = setup()
        attack(d, List(3) { attacker(d) }.associateWith { d.player2 })
        count(d) shouldBe 3
        count(d, Player.You) shouldBe 3
        count(d, Player.EachOpponent) shouldBe 0
    }

    test("an attacker that has left the battlefield still counts") {
        val d = setup()
        val (a, b, c) = List(3) { attacker(d) }
        attack(d, listOf(a, b, c).associateWith { d.player2 })
        d.moveToGraveyard(b)
        count(d) shouldBe 3
    }

    test("a creature put onto the battlefield attacking was never declared and doesn't count") {
        val d = setup()
        attack(d, mapOf(attacker(d) to d.player2))
        val entered = attacker(d)
        d.addComponent(entered, AttackingComponent(d.player2))
        count(d) shouldBe 1
    }

    test("an empty declaration counts zero") {
        val d = setup()
        attacker(d)
        attack(d, emptyMap())
        count(d) shouldBe 0
    }

    test("the record ends with the combat phase, so a second combat starts from zero") {
        val d = setup()
        d.addComponent(d.player1, AdditionalPhasesComponent(listOf(QueuedPhase(ExtraPhaseKind.COMBAT))))
        attack(d, List(2) { attacker(d) }.associateWith { d.player2 })
        count(d) shouldBe 2
        d.passPriorityUntil(Step.END_COMBAT)
        count(d) shouldBe 2 // still this combat during its end of combat step
        d.passPriorityUntil(Step.BEGIN_COMBAT) // the queued combat came first; the natural one follows
        count(d) shouldBe 0
        d.state.getEntity(d.player1)?.has<PlayerAttackersThisCombatComponent>() shouldBe false
        attack(d, mapOf(attacker(d) to d.player2))
        count(d) shouldBe 1
    }

    test("in a shared-team declaration each controller is credited and Each sums the team") {
        val d = setup(players = 4, teamGame = true)
        val p = d.state.turnOrder
        val own = attacker(d, p[0])
        val teammate = attacker(d, p[1])
        attack(d, mapOf(own to p[2], teammate to p[3]), by = p[0])
        count(d, Player.You, p[0]) shouldBe 1
        count(d, Player.You, p[1]) shouldBe 1
        count(d) shouldBe 2
    }
})
