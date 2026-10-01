package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "You may exert this creature as it attacks. When you do, …" — [ExertAsItAttacks] plus its linked
 * `Triggers.self.exertedAsItAttacks()` trigger.
 *
 * - CR 508.1g / 701.43d: exert-as-it-attacks is an *optional* cost to attack, chosen while
 *   attackers are declared — the player may decline it and still attack.
 * - CR 701.43a: an exerted creature doesn't untap during its controller's next untap step.
 * - CR 701.43b: a creature can be exerted even if it's already exerted — the choice is still offered.
 * - CR 607.2h: the "when you do" trigger is linked to the static; exerting the creature some other
 *   way (an activated ability's exert cost) doesn't fire it.
 */
class ExertAsItAttacksTest : FunSpec({

    val trainee = card("Exert Trainee") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 2
        staticAbility { ability = ExertAsItAttacks }
        triggeredAbility {
            trigger = Triggers.self.exertedAsItAttacks()
            effect = Effects.GainLife(3)
        }
        // A second, unrelated way to exert it — must not fire the linked trigger (CR 607.2h).
        activatedAbility {
            cost = Costs.Exert
            effect = Effects.DrawCards(1)
        }
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + trainee)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        return Triple(d, active, d.getOpponent(active))
    }

    fun attack(d: GameTestDriver, active: EntityId, defender: EntityId, attackers: List<EntityId>) {
        attackers.forEach(d::removeSummoningSickness)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, attackers, defender).outcome.shouldBeInstanceOf<Outcome.Paused>()
    }

    /** Walk the turn cycle until [player] is active again and past their untap step. */
    fun advanceToOwnNextTurn(d: GameTestDriver, player: EntityId) {
        d.passPriorityUntil(Step.END)
        d.bothPass()
        while (d.activePlayer != player) {
            d.passPriorityUntil(Step.END)
            d.bothPass()
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("exerting as it attacks fires the linked trigger and skips the next untap") {
        val (d, active, defender) = setup()
        val creature = d.putCreatureOnBattlefield(active, "Exert Trainee")
        attack(d, active, defender, listOf(creature))

        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe active
        decision.options shouldContainExactly listOf(creature)
        decision.minSelections shouldBe 0

        d.submitCardSelection(active, listOf(creature))
        d.state.getEntity(creature)?.has<AttackingComponent>() shouldBe true
        d.state.getEntity(creature)?.has<ExertedComponent>() shouldBe true

        val life = d.getLifeTotal(active)
        d.bothPass() // resolve the "when you do" trigger
        d.getLifeTotal(active) shouldBe life + 3

        advanceToOwnNextTurn(d, active)
        d.isTapped(creature) shouldBe true
        d.state.getEntity(creature)?.has<ExertedComponent>() shouldBe false
    }

    test("declining the exert still attacks, fires nothing, and untaps normally") {
        val (d, active, defender) = setup()
        val creature = d.putCreatureOnBattlefield(active, "Exert Trainee")
        attack(d, active, defender, listOf(creature))

        val life = d.getLifeTotal(active)
        d.submitCardSelection(active, emptyList())
        d.state.getEntity(creature)?.has<AttackingComponent>() shouldBe true
        d.state.getEntity(creature)?.has<ExertedComponent>() shouldBe false
        d.state.stack.isEmpty() shouldBe true
        d.getLifeTotal(active) shouldBe life

        advanceToOwnNextTurn(d, active)
        d.isTapped(creature) shouldBe false
    }

    test("only attackers carrying the ability are offered; others attack without a prompt") {
        val (d, active, defender) = setup()
        val creature = d.putCreatureOnBattlefield(active, "Exert Trainee")
        val bears = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        attack(d, active, defender, listOf(creature, bears))
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldContainExactly listOf(creature)

        // Picking an attacker that wasn't offered is rejected.
        (d.submitCardSelection(active, listOf(bears)).error != null) shouldBe true
    }

    test("an attack with no exert-capable attacker declares without pausing") {
        val (d, active, defender) = setup()
        val bears = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(bears), defender).outcome shouldBe Outcome.Done
        d.state.getEntity(bears)?.has<AttackingComponent>() shouldBe true
    }

    test("an already-exerted creature can be exerted again as it attacks (CR 701.43b)") {
        val (d, active, defender) = setup()
        val creature = d.putCreatureOnBattlefield(active, "Exert Trainee")
        d.replaceState(d.state.updateEntity(creature) { it.with(ExertedComponent) })
        attack(d, active, defender, listOf(creature))
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldContainExactly listOf(creature)

        val life = d.getLifeTotal(active)
        d.submitCardSelection(active, listOf(creature))
        d.bothPass()
        d.getLifeTotal(active) shouldBe life + 3
    }

    test("exerting it to pay an activated ability's cost doesn't fire the linked trigger (CR 607.2h)") {
        val (d, active, _) = setup()
        val creature = d.putCreatureOnBattlefield(active, "Exert Trainee")
        val life = d.getLifeTotal(active)
        d.submitSuccess(
            ActivateAbility(playerId = active, sourceId = creature, abilityId = trainee.activatedAbilities[0].id)
        )
        d.state.getEntity(creature)?.has<ExertedComponent>() shouldBe true
        d.bothPass() // resolve the draw; nothing else should be waiting
        d.state.stack.isEmpty() shouldBe true
        d.getLifeTotal(active) shouldBe life
    }
})
