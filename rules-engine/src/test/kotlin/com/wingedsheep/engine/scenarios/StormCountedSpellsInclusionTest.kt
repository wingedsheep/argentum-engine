package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
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
 * What counts toward Storm's copy count. Per CR 702.40a Storm copies the spell
 * "for each other spell that was cast before it this turn" — so only actual
 * castings count, regardless of whether those spells later resolved or fizzled.
 *
 * Inclusion rules:
 *  - Countered spells still count (they were cast).
 *  - Lands played do NOT count (playing a land is not casting a spell).
 *  - Activated mana abilities do NOT count.
 */
class StormCountedSpellsInclusionTest : FunSpec({

    test("casting a spell bumps spellsCastThisTurn even before resolution") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putLandOnBattlefield(caster, "Mountain")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(opponent)).isSuccess shouldBe true

        // Lightning Bolt is on the stack, not yet resolved — spellsCastThisTurn already reflects it.
        driver.state.spellsCastThisTurn shouldBe 1
    }

    test("playing a land does NOT bump spellsCastThisTurn (Storm count stays zero)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Play a land (not a cast).
        val landInHand = driver.putCardInHand(caster, "Swamp")
        driver.submit(PlayLand(caster, landInHand)).isSuccess shouldBe true
        driver.state.spellsCastThisTurn shouldBe 0

        // Ensure we have enough mana for Tendrils (2BB).
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }

        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        val stormTriggers = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 1
        (stormTriggers.single().effect as StormCopyEffect).copyCount shouldBe 0
    }

    test("activating a mana ability does NOT bump spellsCastThisTurn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        // Put a Swamp on the battlefield and tap it via giveMana (simulates tap-for-mana
        // activation; state should record no spells cast regardless).
        driver.putLandOnBattlefield(caster, "Swamp")
        driver.giveMana(caster, com.wingedsheep.sdk.core.Color.BLACK, 1)

        driver.state.spellsCastThisTurn shouldBe 0
    }
})
