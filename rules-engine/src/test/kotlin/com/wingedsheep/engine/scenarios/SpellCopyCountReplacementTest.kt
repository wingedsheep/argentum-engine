package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.CopyForOtherPossibleTargetsExecutor
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.effects.CopyForOtherPossibleTargetsEffect
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spell-copy count replacement — [ModifyKeywordActionAmount] over [EventPattern.SpellCopiedEvent]:
 * "If you would copy a spell one or more times, instead copy it that many times plus an additional
 * time. You may choose new targets for the additional copy." (Twinning Staff).
 *
 * Covers each spell-copy instruction the engine carries out — copy target spell, storm, the
 * Zada-family "for each other target", chain copies — plus the rulings: instances stack, only
 * the copier's own replacements apply, zero copies stays zero, and the additional copy may be
 * retargeted.
 */
class SpellCopyCountReplacementTest : FunSpec({

    val staff = card("Test Copy Staff") {
        manaCost = "{3}"; typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.SpellCopiedEvent(), 1))
    }
    val instantStaff = card("Test Instant Copy Staff") {
        manaCost = "{3}"; typeLine = "Artifact"
        replacementEffect(
            ModifyKeywordActionAmount(EventPattern.SpellCopiedEvent(spellFilter = GameObjectFilter.Instant), 1)
        )
    }
    val lifeInstant = card("Test Copy Life Instant") {
        manaCost = "{U}"; typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }
    val lifeSorcery = card("Test Copy Life Sorcery") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }
    val stormLife = card("Test Storm Life") {
        manaCost = "{U}"; typeLine = "Sorcery"
        keywords(Keyword.STORM)
        spell { effect = Effects.GainLife(1) }
    }
    val ping = card("Test Copy Ping") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val twincast = card("Test Twincast") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            val s = target(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyTargetSpell(s)
        }
    }
    val noCopy = card("Test Zero Twincast") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            val s = target(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyTargetSpell(s, copies = DynamicAmount.Fixed(0))
        }
    }
    val chainPing = card("Test Chain Ping") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val t = target(Targets.Any)
            effect = Effects.ChainCopy(
                action = Effects.DealDamage(1, t),
                target = t,
                offerTo = CopyRecipient.AFFECTED_PLAYER,
                copyTarget = Targets.Any
            )
        }
    }

    val howl = card("Test Copy Howl") {
        manaCost = "{U}"; typeLine = "Instant"
        spell { effect = Effects.CopyNextSpellCast(copies = 2, spellFilter = GameObjectFilter.Sorcery) }
    }
    val copyEach = card("Test Copy Each") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            target(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyEachTargetSpell()
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(
            TestCards.all + listOf(staff, instantStaff, lifeInstant, lifeSorcery, stormLife, ping, twincast, noCopy, chainPing, howl, copyEach)
        )
        it.initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String, targets: List<ChosenTarget> = emptyList()): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        castSpellWithTargets(player1, id, targets).error shouldBe null
        return id
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && !isPaused && guard++ < 50) bothPass()
    }

    fun GameTestDriver.copiesOnStack() = state.stack.count { state.getEntity(it)?.has<CopyOfComponent>() == true }

    /** Cast the life spell, copy it with Twincast, resolve everything; returns life gained. */
    fun GameTestDriver.twincastLifeGain(spell: String = lifeSorcery.name, copier: String = twincast.name): Int {
        val before = getLifeTotal(player1)
        val original = cast(spell)
        cast(copier, listOf(ChosenTarget.Spell(original)))
        resolveAll()
        return getLifeTotal(player1) - before
    }

    test("without a copy-count replacement, copy target spell makes one copy") {
        driver().twincastLifeGain() shouldBe 2
    }

    test("copy target spell makes one additional copy") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        d.twincastLifeGain() shouldBe 3
    }

    test("each instance adds its own additional copy") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        d.putPermanentOnBattlefield(d.player1, staff.name)
        d.twincastLifeGain() shouldBe 4
    }

    test("an opponent's replacement does not resize copies you make") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, staff.name)
        d.twincastLifeGain() shouldBe 2
    }

    test("zero copies is no copy and is never resized") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        d.twincastLifeGain(copier = noCopy.name) shouldBe 1
    }

    test("the pattern's spell filter is matched against the copied spell") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, instantStaff.name)
        d.twincastLifeGain(spell = lifeSorcery.name) shouldBe 2
        d.twincastLifeGain(spell = lifeInstant.name) shouldBe 3
    }

    test("storm's copy instruction is resized once") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        val before = d.getLifeTotal(d.player1)
        d.cast(lifeInstant.name)
        d.resolveAll()
        // Storm count 1 → one copy, plus the additional one: the storm spell resolves three times.
        d.cast(stormLife.name)
        d.resolveAll()
        d.getLifeTotal(d.player1) - before shouldBe 1 + 3
    }

    test("each copy-the-next-spell rider is its own instruction and is resized separately") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        d.cast(howl.name); d.resolveAll()
        d.cast(howl.name); d.resolveAll()
        val before = d.getLifeTotal(d.player1)
        d.cast(lifeSorcery.name)
        d.resolveAll()
        // Two riders of two copies each, each plus one: the original and six copies.
        d.getLifeTotal(d.player1) - before shouldBe 1 + 3 + 3
    }

    test("copy each target spell resizes each spell's copy") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        d.twincastLifeGain(copier = copyEach.name) shouldBe 3
    }

    test("every copy, the additional one included, may be given new targets") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val original = d.cast(ping.name, listOf(ChosenTarget.Player(d.player2)))
        d.cast(twincast.name, listOf(ChosenTarget.Spell(original)))
        d.bothPass() // Twincast resolves

        val first = d.pendingDecision as ChooseTargetsDecision
        first.prompt shouldBe "Choose new targets for copy 1 of 2 of ${ping.name}"
        d.submitTargetSelection(d.player1, emptyList()).error shouldBe null
        (d.pendingDecision as ChooseTargetsDecision).prompt shouldBe "Choose new targets for copy 2 of 2 of ${ping.name}"
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null

        val copyTargets = d.state.stack
            .filter { d.state.getEntity(it)?.has<CopyOfComponent>() == true }
            .map { d.state.getEntity(it)!!.get<TargetsComponent>()!!.targets.single() }
            .toSet()
        copyTargets shouldBe setOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(bear))
    }

    test("copy for each other target adds a retargetable copy of the original") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, staff.name)
        val aimed = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val c = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val original = d.cast(ping.name, listOf(ChosenTarget.Permanent(aimed)))

        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val result = CopyForOtherPossibleTargetsExecutor(TargetFinder(predicates), predicates).execute(
            d.state,
            CopyForOtherPossibleTargetsEffect(
                target = EffectTarget.ContextTarget(0),
                candidates = GameObjectFilter.Creature.youControl()
            ),
            EffectContext(sourceId = null, controllerId = d.player1, targets = listOf(ChosenTarget.Spell(original)))
        )
        result.error shouldBe null
        d.replaceState(result.state)

        // The two candidate copies are on the stack; the additional one waits on its target choice.
        d.copiesOnStack() shouldBe 2
        (d.pendingDecision as ChooseTargetsDecision).prompt shouldBe "Choose new targets for copy of ${ping.name}"
        d.submitTargetSelection(d.player1, emptyList()).error shouldBe null

        d.copiesOnStack() shouldBe 3
        d.state.stack
            .filter { d.state.getEntity(it)?.has<CopyOfComponent>() == true }
            .map { d.state.getEntity(it)!!.get<TargetsComponent>()!!.targets.single() }
            .toSet() shouldBe setOf(ChosenTarget.Permanent(b), ChosenTarget.Permanent(c), ChosenTarget.Permanent(aimed))
    }

    test("a chain copy is resized for the player who copies it") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, staff.name)
        d.cast(chainPing.name, listOf(ChosenTarget.Player(d.player2)))
        d.bothPass() // Chain Ping resolves; its target's player is offered the copy

        d.pendingDecision as YesNoDecision
        d.submitYesNo(d.player2, true).error shouldBe null
        (d.pendingDecision as SelectCardsDecision).options.contains(d.player1) shouldBe true
        d.submitCardSelection(d.player2, listOf(d.player1)).error shouldBe null
        // The additional copy picks its own target.
        d.submitCardSelection(d.player2, listOf(d.player1)).error shouldBe null

        d.isPaused shouldBe false
        d.state.stack.count {
            d.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.controllerId == d.player2
        } shouldBe 2
    }
})
