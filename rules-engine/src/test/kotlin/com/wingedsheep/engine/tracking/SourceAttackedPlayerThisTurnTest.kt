package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The per-creature record behind `Conditions.SourceAttackedPlayerThisTurn`: which players a
 * creature was declared attacking directly this turn, scoped to that object and that turn.
 */
class SourceAttackedPlayerThisTurnTest : FunSpec({
    val walker = card("Attacked Walker Probe") { typeLine = "Planeswalker — Test"; startingLoyalty = 20 }
    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(walker))
        initMultiplayer(List(4) { Deck.of("Forest" to 40) }, format = Format.Standard)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val evaluator = PredicateEvaluator(cardRegistry = null)

    /** Asks the condition of [player] with [source] as the ability's source. */
    fun attacked(d: GameTestDriver, source: EntityId, player: EntityId): Boolean =
        evaluator.conditions.evaluate(
            d.state,
            Conditions.SourceAttackedPlayerThisTurn(Player.You),
            EffectContext(source, player),
        )
    fun attacker(d: GameTestDriver): EntityId =
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also(d::removeSummoningSickness)
    fun attack(d: GameTestDriver, assignments: Map<EntityId, EntityId>) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, assignments).error shouldBe null
    }

    test("names only the player this creature attacked, not those its teammates attacked") {
        val d = setup()
        val p = d.state.turnOrder
        val bears = attacker(d)
        val other = attacker(d)
        attack(d, mapOf(bears to p[1], other to p[2]))
        attacked(d, bears, p[1]) shouldBe true
        attacked(d, bears, p[2]) shouldBe false
        attacked(d, bears, p[3]) shouldBe false
        attacked(d, other, p[2]) shouldBe true
    }

    test("attacking a planeswalker does not attack its controller") {
        val d = setup()
        val p = d.state.turnOrder
        val pw = d.putPermanentOnBattlefield(p[1], walker.name)
        d.addComponent(pw, CountersComponent().withAdded(CounterType.LOYALTY, 20))
        val bears = attacker(d)
        attack(d, mapOf(bears to pw))
        attacked(d, bears, p[1]) shouldBe false
    }

    test("a second combat adds its defender to the record") {
        val d = setup()
        val p = d.state.turnOrder
        d.addComponent(d.player1, AdditionalPhasesComponent(listOf(QueuedPhase(ExtraPhaseKind.COMBAT))))
        val bears = attacker(d)
        attack(d, mapOf(bears to p[1]))
        d.passPriorityUntil(Step.END_COMBAT) // the queued combat came first; the natural one follows
        d.untapPermanent(bears)
        attack(d, mapOf(bears to p[2]))
        attacked(d, bears, p[1]) shouldBe true
        attacked(d, bears, p[2]) shouldBe true
    }

    test("the record survives the end of combat but not the end of the turn") {
        val d = setup()
        val p = d.state.turnOrder
        val bears = attacker(d)
        attack(d, mapOf(bears to p[1]))
        d.passPriorityUntil(Step.END)
        attacked(d, bears, p[1]) shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        attacked(d, bears, p[1]) shouldBe false
    }
})
