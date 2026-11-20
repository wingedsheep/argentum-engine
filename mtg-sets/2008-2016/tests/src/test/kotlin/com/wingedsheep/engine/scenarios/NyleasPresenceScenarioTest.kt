package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.NyleasPresence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nylea's Presence (THS #169): the first Aura to grant land subtypes through the attached-to scope.
 * Pins that only the enchanted land gains all five basic land types, that it then taps for every
 * colour through the intrinsic mana abilities derived from those projected subtypes, and that the
 * enters trigger draws a card.
 */
class NyleasPresenceScenarioTest : FunSpec({

    val basicTypes = listOf("Plains", "Island", "Swamp", "Mountain", "Forest")

    test("the enchanted land is every basic land type, taps for any colour, and the Aura draws a card") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + NyleasPresence)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val enchanted = driver.putLandOnBattlefield(me, "Plains")
        val other = driver.putLandOnBattlefield(me, "Plains")
        val presence = driver.putCardInHand(me, "Nylea's Presence")
        val handBefore = driver.getHandSize(me)

        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, presence, targets = listOf(enchanted)).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the Aura
        driver.findPermanent(me, "Nylea's Presence") shouldNotBe null
        driver.bothPass() // resolve the enters trigger

        // Cast the Aura (-1), drew a card (+1).
        driver.getHandSize(me) shouldBe handBefore

        val projected = driver.state.projectedState
        basicTypes.forEach { projected.hasSubtype(enchanted, it).shouldBeTrue() }
        basicTypes.filter { it != "Plains" }.forEach { projected.hasSubtype(other, it).shouldBeFalse() }

        val offered = driver.legalActions(me).filter { it.affordable }.map { it.action }
            .filterIsInstance<ActivateAbility>().filter { it.sourceId == enchanted }.map { it.abilityId }.toSet()
        "WUBRG".forEach { offered.contains(AbilityId.intrinsicMana(it)).shouldBeTrue() }

        driver.submitSuccess(ActivateAbility(me, enchanted, AbilityId.intrinsicMana('B')))
        driver.state.getEntity(me)?.get<ManaPoolComponent>()?.black shouldBe 1
        driver.isTapped(enchanted).shouldBeTrue()
    }
})
