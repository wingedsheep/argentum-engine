package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.StaticAbilityGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A static ability handed out at runtime through `GrantStaticAbilityEffect` ("target creature
 * gains 'This creature can't block' until end of turn") must reach the layer system exactly as the
 * same ability printed on the card does. Projection used to read only one granted kind
 * (attack-as-though-hasty) and drop every other projected static on the floor.
 */
class GrantedProjectedStaticTest : FunSpec({
    val holder = card("Granted Static Holder") { typeLine = "Enchantment" }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(holder))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.grant(to: EntityId, ability: StaticAbility, duration: Duration = Duration.EndOfTurn) {
        val result = services.effectExecutorRegistry.execute(
            state,
            Effects.GrantStaticAbility(ability, EffectTarget.ContextTarget(0), duration),
            EffectContext(sourceId = to, controllerId = activePlayer!!, targets = listOf(ChosenTarget.Permanent(to)))
        )
        result.error shouldBe null
        result.events shouldBe listOf(StaticAbilityGrantedEvent(to))
        replaceState(result.state)
    }

    test("a granted self-scoped CantBlock stops the creature from blocking") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.grant(blocker, CantBlock())
        d.state.projectedState.cantBlock(blocker) shouldBe true

        var next = d.state.copy(step = Step.DECLARE_BLOCKERS, priorityPlayerId = opp)
        next = next.updateEntity(attacker) { it.with(AttackingComponent(opp)) }
        d.replaceState(next)
        d.declareBlockers(opp, mapOf(blocker to listOf(attacker))).error shouldNotBe null
    }

    test("a granted self-scoped CantAttack is projected") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.state.projectedState.cantAttack(bears) shouldBe false
        d.grant(bears, CantAttack())
        d.state.projectedState.cantAttack(bears) shouldBe true
    }

    test("a granted group static applies to every creature it names") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(me, holder.name)
        val mine = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.grant(source, CantBlock(GroupFilter(GameObjectFilter.Creature)))
        d.state.projectedState.cantBlock(mine) shouldBe true
        d.state.projectedState.cantBlock(theirs) shouldBe true
    }

    test("a granted P/T static applies once") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.grant(bears, ModifyStats(2, 2, GroupFilter.source()))
        d.state.projectedState.getPower(bears) shouldBe 4
        d.state.projectedState.getToughness(bears) shouldBe 4
    }

    test("a token's own statics are not applied twice") {
        val d = driver(); val me = d.activePlayer!!
        val result = d.services.effectExecutorRegistry.execute(
            d.state,
            Effects.CreateToken(
                power = 1, toughness = 1, creatureTypes = setOf("Soldier"),
                staticAbilities = listOf(ModifyStats(1, 1, GroupFilter.source()))
            ),
            EffectContext(sourceId = null, controllerId = me)
        )
        result.error shouldBe null
        d.replaceState(result.state)
        val token = d.state.getBattlefield().single { id ->
            d.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.typeLine?.subtypes?.any { it.value == "Soldier" } == true
        }
        d.state.projectedState.getPower(token) shouldBe 2
    }

    test("the granted static ends with its duration") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.grant(blocker, CantBlock())
        d.state.projectedState.cantBlock(blocker) shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.cantBlock(blocker) shouldBe false
    }
})
