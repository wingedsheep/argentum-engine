package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "You attacked with a Rogue this turn" reads each attacker as it was declared (CR 508.1), not as it
 * is now: the record survives the creature ceasing to exist or changing its types, and a creature
 * that only became a Rogue after attacking doesn't count (Robber of the Rich rulings).
 */
class AttackedWithCreaturesAsDeclaredTest : FunSpec({
    val rogue = card("Declared Rogue Probe") { typeLine = "Creature — Human Rogue"; power = 2; toughness = 2 }
    val evaluator = PredicateEvaluator(cardRegistry = null)
    val attackedWithRogue = Conditions.YouAttackedWithCreaturesThisTurn(
        filter = GameObjectFilter.Creature.withSubtype("Rogue"),
        atLeast = 1
    )

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + rogue)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun creature(d: GameTestDriver, name: String): EntityId =
        d.putCreatureOnBattlefield(d.player1, name).also { d.removeSummoningSickness(it) }
    fun attack(d: GameTestDriver, attacker: EntityId) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, mapOf(attacker to d.player2)).error shouldBe null
    }
    fun attackedWithRogue(d: GameTestDriver): Boolean =
        evaluator.conditions.evaluate(d.state, attackedWithRogue, EffectContext(null, d.player1))
    fun retype(d: GameTestDriver, id: EntityId, typeLine: String) {
        val card = d.state.getEntity(id)!!.get<CardComponent>()!!
        d.addComponent(id, card.copy(typeLine = TypeLine.parse(typeLine)))
    }

    test("a Rogue that attacked still counts after it ceases to exist") {
        val d = setup()
        val id = creature(d, rogue.name)
        attack(d, id)
        attackedWithRogue(d) shouldBe true
        d.replaceState(d.state.removeEntity(id))
        attackedWithRogue(d) shouldBe true
    }

    test("a creature that stops being a Rogue after attacking still counts") {
        val d = setup()
        val id = creature(d, rogue.name)
        attack(d, id)
        retype(d, id, "Creature — Human")
        attackedWithRogue(d) shouldBe true
    }

    test("a creature that becomes a Rogue only after attacking doesn't count") {
        val d = setup()
        val id = creature(d, "Grizzly Bears")
        attack(d, id)
        retype(d, id, "Creature — Bear Rogue")
        attackedWithRogue(d) shouldBe false
    }

    test("a Rogue that didn't attack doesn't count") {
        val d = setup()
        creature(d, rogue.name)
        attack(d, creature(d, "Grizzly Bears"))
        attackedWithRogue(d) shouldBe false
    }
})
