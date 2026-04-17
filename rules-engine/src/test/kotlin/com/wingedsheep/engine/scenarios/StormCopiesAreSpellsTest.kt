package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.TendrilsOfAgony
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Phase 1 of `backlog/storm-implementation-correctness.md`: Storm copies are
 * now real spell-on-stack entities (`SpellOnStackComponent` + `CopyOfComponent`),
 * so "target spell" effects like Counterspell must be able to find them in the
 * stack-target enumeration.
 */
class StormCopiesAreSpellsTest : FunSpec({

    test("a Storm copy is enumerated by findValidSpellTargets(TargetFilter.SpellOnStack)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        // Drive the Storm copy onto the stack: resolve the Storm trigger and supply a target.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        driver.submitTargetSelection(caster, listOf(opponent)).isSuccess shouldBe true

        // Identify the copy entity.
        val copyId = driver.state.stack.single { id ->
            val c = driver.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }

        // Target enumeration for "target spell" must include the copy.
        val enumerator = TargetEnumerationUtils(PredicateEvaluator())
        val targetable = enumerator.findValidSpellTargets(
            driver.state,
            opponent,
            TargetFilter.SpellOnStack
        )
        targetable.contains(copyId) shouldBe true
    }

    test("the original Tendrils and its Storm copy are both on the stack as spells") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        driver.submitTargetSelection(caster, listOf(opponent)).isSuccess shouldBe true

        val spellLikeOnStack = driver.state.stack.filter { id ->
            driver.state.getEntity(id)?.get<SpellOnStackComponent>() != null
        }
        // Original Tendrils + the copy, both as SpellOnStackComponent.
        spellLikeOnStack.contains(tendrils) shouldBe true
        spellLikeOnStack.any { id -> driver.state.getEntity(id)?.has<CopyOfComponent>() == true } shouldBe true
        spellLikeOnStack.size shouldBe 2
    }
})
