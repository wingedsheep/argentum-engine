package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.TendrilsOfAgony
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Baseline integration tests for the Storm keyword (CR 702.40).
 *
 * Covers the "how many copies" contract:
 *  - one non-Storm spell cast beforehand → 1 copy
 *  - no prior spells → trigger still fires (Phase 7) but creates 0 copies
 *  - a Storm copy itself does NOT count toward future Storm counts (CR 707.10)
 */
class StormBasicCountTest : FunSpec({

    test("Storm count = 1 when one other spell was cast earlier this turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Simulate one spell already cast this turn (bypass mana bookkeeping).
        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))

        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        val stormTriggers = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 1
        (stormTriggers.single().effect as StormCopyEffect).copyCount shouldBe 1
    }

    test("Storm trigger fires with copyCount 0 when no other spells have been cast (Phase 7)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // No prior spells — spellsCastThisTurn defaults to 0.
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        val stormTriggers = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 1
        (stormTriggers.single().effect as StormCopyEffect).copyCount shouldBe 0
    }

    test("casting a Storm spell increments spellsCastThisTurn by exactly 1 (the spell itself, not its copies)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.replaceState(driver.state.copy(spellsCastThisTurn = 2))
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        // Storm trigger created 2 copies, but 707.10 — copies aren't cast, so the counter
        // only advances for the original Tendrils spell itself.
        driver.state.spellsCastThisTurn shouldBe 3
    }

    test("Tendrils itself is a SpellOnStackComponent below the Storm trigger") {
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

        // Original Tendrils is on the stack.
        driver.state.getEntity(tendrils)?.get<SpellOnStackComponent>() shouldBe driver.state.getEntity(tendrils)!!.get<SpellOnStackComponent>()
        driver.state.stack.contains(tendrils) shouldBe true
    }
})
