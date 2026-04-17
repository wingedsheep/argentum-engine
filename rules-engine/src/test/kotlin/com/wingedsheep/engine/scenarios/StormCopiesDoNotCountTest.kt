package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.TendrilsOfAgony
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 707.10: "The act of copying a spell or ability is not the same as casting
 * or activating." Storm copies of a spell must not count toward `spellsCastThisTurn`
 * and must not fire "whenever you cast a spell" effects.
 */
class StormCopiesDoNotCountTest : FunSpec({

    test("Tendrils with storm count = 2 only advances spellsCastThisTurn by 1 (the original cast)") {
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

        // Storm created 2 copies. Per 707.10 the copies don't count.
        driver.state.spellsCastThisTurn shouldBe 3
    }

    test("casting a Storm spell emits exactly one SpellCastEvent (the spell itself)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.replaceState(driver.state.copy(spellsCastThisTurn = 3))
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")

        val eventsBefore = driver.events.size
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        val eventsAfter = driver.events.drop(eventsBefore)
        // Storm trigger landing on stack emits AbilityTriggeredEvent, not SpellCastEvent.
        // Only the original Tendrils cast emits a SpellCastEvent.
        eventsAfter.filterIsInstance<SpellCastEvent>().size shouldBe 1
        eventsAfter.filterIsInstance<SpellCastEvent>().single().spellEntityId shouldBe tendrils
    }
})
