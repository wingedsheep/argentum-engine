package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CopyExceptions.retainPowerToughness` — "except its power and toughness are equal to this
 * creature's power and toughness" (Hulking Metamorph). The copy keeps the copier's own pre-copy base
 * P/T on every path that has a copier; the prototype half is covered by HulkingMetamorphScenarioTest.
 */
class RetainedCopyPowerToughnessTest : FunSpec({
    val copier = card("Test Sized Copier") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 5; toughness = 6
        replacementEffect(EntersAsCopy(exceptions = CopyExceptions(retainPowerToughness = true)))
    }
    val plainClone = card("Test Plain Clone") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(EntersAsCopy())
    }
    val reanimate = card("Test Sized Reanimate") {
        manaCost = "{B}"; typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CreatureInGraveyard)
            effect = Effects.PutOntoBattlefield(t)
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, plainClone, reanimate))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!

    test("entering as a copy keeps its own 5/6, and a plain Clone of it copies that 5/6") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, copier.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass()
        d.submitCardSelection(d.player1, listOf(bear)).error shouldBe null
        card(d, spell).name shouldBe "Grizzly Bears"
        d.state.projectedState.getPower(spell) shouldBe 5
        d.state.projectedState.getToughness(spell) shouldBe 6

        // The retained size is a copiable value (CR 707.9b).
        val clone = d.putCardInHand(d.player1, plainClone.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, clone).error shouldBe null
        d.bothPass()
        d.submitCardSelection(d.player1, listOf(spell)).error shouldBe null
        d.state.projectedState.getPower(clone) shouldBe 5
        d.state.projectedState.getToughness(clone) shouldBe 6
    }

    test("effect-driven entry keeps the entrant's printed size") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val entrant = d.putCardInGraveyard(d.player1, copier.name)
        val spell = d.putCardInHand(d.player1, reanimate.name)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(entrant, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(giant)).error shouldBe null
        card(d, entrant).name shouldBe "Hill Giant"
        d.state.projectedState.getPower(entrant) shouldBe 5
        d.state.projectedState.getToughness(entrant) shouldBe 6
    }

    test("an existing permanent becoming a copy keeps its current base size") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val result = EachPermanentBecomesCopyOfTargetExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry), d.cardRegistry)
            .execute(d.state, EachPermanentBecomesCopyOfTargetEffect(
                target = EffectTarget.ContextTarget(0), affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainPowerToughness = true)),
                EffectContext(sourceId = bear, controllerId = d.player1, targets = listOf(ChosenTarget.Permanent(giant))))
        d.replaceState(result.state)
        card(d, bear).name shouldBe "Hill Giant"
        d.state.projectedState.getPower(bear) shouldBe 2
        d.state.projectedState.getToughness(bear) shouldBe 2
    }

    test("can't combine with a stated power/toughness override") {
        shouldThrow<IllegalArgumentException> { CopyExceptions(retainPowerToughness = true, powerOverride = 4) }
    }

    test("layering over a stated size resolves to whichever side said it last, never both") {
        val retain = CopyExceptions(retainPowerToughness = true)
        val sized = CopyExceptions(powerOverride = 4, toughnessOverride = 4)
        retain.over(sized) shouldBe retain
        sized.over(retain) shouldBe sized
    }
})
