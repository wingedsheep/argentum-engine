package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Overload (CR 702.96) — an alternative cost whose payment changes "target" to "each".
 *
 * - CR 702.96a: casting for the overload cost pays it *instead of* the mana cost, and the spell
 *   resolves with its "each" text.
 * - CR 702.96b: an overloaded spell has no targets, so it is castable when nothing could be
 *   targeted and it affects objects that couldn't have been targeted (hexproof).
 * - CR 118.9c: paying an alternative cost doesn't change the spell's mana value.
 * - Without the overload cost, the spell is the ordinary single-target spell.
 */
class OverloadTest : FunSpec({

    val zap = card("Overload Zap") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.overload("{2}{R}{R}"))
        spell {
            val creature = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.DealDamage(2, creature)
            overloadEffect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesOpponentsControl,
                effect = Effects.DealDamage(2, EffectTarget.IterationEntity)
            )
        }
    }

    val shroudedBear = card("Hexproof Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.HEXPROOF)
    }

    class Table(val d: GameTestDriver, val me: EntityId, val opp: EntityId, val zapId: EntityId)

    fun setup(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + zap + shroudedBear)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        return Table(d, me, d.getOpponent(me), d.putCardInHand(me, "Overload Zap"))
    }

    fun Table.overload(targets: List<ChosenTarget> = emptyList()) = d.submit(
        CastSpell(
            me, zapId, targets,
            paymentStrategy = PaymentStrategy.FromPool,
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.OVERLOAD,
        )
    )

    test("overloaded, the spell affects each matching creature — hexproof included — and nothing else") {
        val t = setup()
        val theirBears = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        val theirHexproof = t.d.putCreatureOnBattlefield(t.opp, "Hexproof Bear")
        val myBears = t.d.putCreatureOnBattlefield(t.me, "Grizzly Bears")
        t.d.giveMana(t.me, Color.RED, 4)

        t.overload().error shouldBe null
        t.d.state.getEntity(t.zapId)?.get<SpellOnStackComponent>()?.wasOverloaded shouldBe true
        t.d.state.getEntity(t.zapId)?.get<TargetsComponent>() shouldBe null
        t.d.bothPass()

        t.d.state.getBattlefield().contains(theirBears) shouldBe false
        t.d.state.getBattlefield().contains(theirHexproof) shouldBe false
        t.d.state.getBattlefield().contains(myBears) shouldBe true
    }

    test("the overload cast is offered, untargeted, even when the printed cast has no legal target") {
        val t = setup()
        t.d.putCreatureOnBattlefield(t.opp, "Hexproof Bear")
        t.d.giveMana(t.me, Color.RED, 4)

        val castsOfZap = t.d.legalActions(t.me).filter { (it.action as? CastSpell)?.cardId == t.zapId }
        val overload = castsOfZap.single { (it.action as CastSpell).alternativeCostType == AlternativeCostType.OVERLOAD }
        overload.requiresTargets shouldBe false
        overload.affordable shouldBe true
        castsOfZap.none { !(it.action as CastSpell).useAlternativeCost } shouldBe true
    }

    test("the overload cost replaces the mana cost: the printed {R} alone can't pay it") {
        val t = setup()
        t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        t.d.giveMana(t.me, Color.RED, 1)

        val overload = t.d.legalActions(t.me).single {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.OVERLOAD
        }
        overload.affordable shouldBe false
        t.overload().error shouldNotBe null
    }

    test("an overloaded cast that names targets is rejected") {
        val t = setup()
        val theirBears = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        t.d.giveMana(t.me, Color.RED, 4)

        t.overload(listOf(ChosenTarget.Permanent(theirBears))).error shouldNotBe null
    }

    test("paying the overload cost doesn't change the spell's mana value") {
        val t = setup()
        t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        t.d.giveMana(t.me, Color.RED, 4)

        val cast = t.overload()
        cast.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe 1
    }

    test("cast normally, it is the single-target spell") {
        val t = setup()
        val first = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        val second = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        t.d.giveMana(t.me, Color.RED, 1)

        t.d.castSpell(t.me, t.zapId, listOf(first)).error shouldBe null
        t.d.state.getEntity(t.zapId)?.get<SpellOnStackComponent>().shouldNotBeNull().wasOverloaded shouldBe false
        t.d.bothPass()

        t.d.state.getBattlefield().contains(first) shouldBe false
        t.d.state.getBattlefield().contains(second) shouldBe true
    }
})
