package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellExecutor
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A flat target list keeps the boundaries between a spell's instances of the word "target"
 * (CR 601.2c) even when an "up to N" group is only partly filled — announced explicitly
 * (`targetGroupCounts`) or inferred — and every later reader (named targets, the stack's recorded
 * requirements, splice slices, copies) sees the same split.
 */
class TargetGroupBindingTest : FunSpec({
    // "Up to two target creatures each take 2; target opponent loses 3."
    val partial = card("Test Partial Group Bolt") {
        manaCost = "{R}"; typeLine = "Instant"
        spell {
            val (first, second) = targets(TargetFilter.Creature, count = 2, optional = true)
            val opponent = target(Targets.Opponent)
            effect = Effects.DealDamage(2, first) then Effects.DealDamage(2, second) then
                Effects.LoseLife(3, opponent)
        }
    }
    // "Up to one target creature takes 1; up to one target creature takes 3." — two optional words.
    val twoOptional = card("Test Two Optional Words") {
        manaCost = "{R}"; typeLine = "Instant"
        spell {
            val small = target(TargetFilter.Creature, optional = true)
            val big = target(TargetFilter.Creature, optional = true)
            effect = Effects.DealDamage(1, small) then Effects.DealDamage(3, big)
        }
    }
    // "Choose one or both — • Up to two target creatures each take 1. • Up to one target creature takes 3."
    val modalGroups = card("Test Modal Optional Groups") {
        manaCost = "{R}"; typeLine = "Instant"
        spell {
            modal(chooseCount = 2, minChooseCount = 1) {
                mode("Up to two target creatures each take 1") {
                    val (first, second) = targets(TargetFilter.Creature, count = 2, optional = true)
                    effect = Effects.DealDamage(1, first) then Effects.DealDamage(1, second)
                }
                mode("Up to one target creature takes 3") {
                    val creature = target(TargetFilter.Creature, optional = true)
                    effect = Effects.DealDamage(3, creature)
                }
            }
        }
    }
    val rod = card("Test Partial Group Rod") {
        manaCost = "{1}"; typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val (first, second) = targets(TargetFilter.Creature, count = 2, optional = true)
            val opponent = target(Targets.Opponent)
            effect = Effects.DealDamage(2, first) then Effects.DealDamage(2, second) then
                Effects.LoseLife(3, opponent)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(partial, twoOptional, modalGroups, rod))
        it.initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.cast(name: String, targets: List<ChosenTarget>, counts: List<Int>? = null): Pair<EntityId, String?> {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.RED)
        return id to submit(CastSpell(player1, id, targets = targets, paymentStrategy = PaymentStrategy.FromPool,
            targetGroupCounts = counts)).error
    }

    test("a partly filled up-to group followed by another target word casts and resolves on the right objects") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val (spell, error) = d.cast(partial.name, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(d.player2)))
        error shouldBe null
        d.state.getEntity(spell)!!.get<TargetsComponent>()!!.targetRequirements.map { it.count } shouldBe listOf(1, 1)
        val life = d.getLifeTotal(d.player2)
        d.bothPass().error shouldBe null
        (bears in d.state.getBattlefield()) shouldBe false
        d.getLifeTotal(d.player2) shouldBe life - 3
    }

    test("an empty up-to group binds nothing and the opponent still loses life") {
        val d = driver()
        val (_, error) = d.cast(partial.name, listOf(ChosenTarget.Player(d.player2)))
        error shouldBe null
        val life = d.getLifeTotal(d.player2)
        d.bothPass().error shouldBe null
        d.getLifeTotal(d.player2) shouldBe life - 3
    }

    test("announced counts choose which optional word a lone target fills") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        d.cast(twoOptional.name, listOf(ChosenTarget.Permanent(giant)), counts = listOf(0, 1)).second shouldBe null
        d.bothPass().error shouldBe null
        // The 3 went to the giant (a 3/3), not the 1.
        (giant in d.state.getBattlefield()) shouldBe false
    }

    test("without announced counts a lone target fills the first optional word") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        d.cast(twoOptional.name, listOf(ChosenTarget.Permanent(giant))).second shouldBe null
        d.bothPass().error shouldBe null
        d.damage(giant) shouldBe 1
    }

    test("announced counts that don't fit the targets are rejected") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(d.player2))
        d.cast(partial.name, targets, counts = listOf(2, 0)).second shouldNotBe null
        d.cast(partial.name, targets, counts = listOf(1, 0)).second shouldNotBe null
        d.cast(partial.name, targets, counts = listOf(0, 2)).second shouldNotBe null
        // The opponent can't stand in a creature slot even when the counts say so.
        d.cast(partial.name, listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(bears)), counts = listOf(1, 1))
            .second shouldNotBe null
    }

    test("a copy keeps the bound groups and its new targets resolve on the right objects") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val (spell, error) = d.cast(partial.name, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(d.player2)))
        error shouldBe null
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val copied = CopyTargetSpellExecutor(predicates.conditions.amounts, TargetFinder(predicates)).execute(
            d.state, CopyTargetSpellEffect(EffectTarget.ContextTarget(0), copies = DynamicAmount.Fixed(1),
                exceptions = CopyExceptions(overrideColors = setOf(Color.RED))),
            EffectContext(sourceId = null, controllerId = d.player1, targets = listOf(ChosenTarget.Spell(spell)))
        )
        copied.error shouldBe null
        d.replaceState(copied.state)
        // One slot per recorded target: the creature slot moves to the giant, the opponent stays.
        d.submitTargetSelection(d.player1, listOf(giant)).error shouldBe null
        val copy = d.state.stack.single { d.state.getEntity(it)?.has<CopyOfComponent>() == true }
        d.state.getEntity(copy)!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Permanent(giant), ChosenTarget.Player(d.player2))
        val life = d.getLifeTotal(d.player2)
        d.bothPass().error shouldBe null
        d.damage(giant) shouldBe 2
        d.getLifeTotal(d.player2) shouldBe life - 3
    }

    test("a choose-N modal cast binds each mode's targets to that mode, not to an inferred union split") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = d.putCardInHand(d.player1, modalGroups.name)
        d.giveMana(d.player1, Color.RED)
        // The shape the engine-driven per-mode target prompt finalizes with: per-mode slices plus
        // their flat union. A greedy split of the union would hand both creatures to the first mode.
        d.submit(CastSpell(d.player1, id, targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
            chosenModes = listOf(0, 1),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bears)), listOf(ChosenTarget.Permanent(giant))),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.state.getEntity(id)!!.get<TargetsComponent>()!!.targetRequirements.map { it.count } shouldBe listOf(1, 1)
        d.bothPass().error shouldBe null
        d.damage(bears) shouldBe 1
        (giant in d.state.getBattlefield()) shouldBe false
    }

    test("a partly filled group's creature leaving before resolution still lets the opponent's slot resolve") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val (spell, error) = d.cast(partial.name, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(d.player2)))
        error shouldBe null
        d.moveToGraveyard(bears)
        val life = d.getLifeTotal(d.player2)
        d.bothPass().error shouldBe null
        (spell in d.state.stack) shouldBe false
        d.getLifeTotal(d.player2) shouldBe life - 3
    }

    test("an activated ability binds a partly filled up-to group the same way") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, rod.name)
        val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.RED)
        d.submit(ActivateAbility(d.player1, source, rod.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(d.player2)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        val life = d.getLifeTotal(d.player2)
        d.bothPass().error shouldBe null
        (bears in d.state.getBattlefield()) shouldBe false
        d.getLifeTotal(d.player2) shouldBe life - 3
    }
})
