package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.Ornithopter
import com.wingedsheep.mtg.sets.definitions.thb.cards.HatefulEidolon
import com.wingedsheep.mtg.sets.definitions.thb.cards.OneWithTheStars
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * One with the Stars — "Enchanted permanent is an enchantment and loses all other card types."
 *
 * Rulings: it keeps its abilities and supertypes, and loses the subtypes of the card types it lost
 * (CR 205.1a) — an enchantment creature keeps being an enchantment but stops being a Spirit.
 */
class OneWithTheStarsScenarioTest : FunSpec({

    fun enchant(permanentName: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OneWithTheStars, Ornithopter, HatefulEidolon))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val permanent = driver.putPermanentOnBattlefield(player, permanentName)
        val aura = driver.putCardInHand(player, "One with the Stars")
        driver.giveMana(player, Color.BLUE, 4)
        driver.castSpell(player, aura, listOf(permanent))
        driver.bothPass()
        return driver to permanent
    }

    test("an artifact creature becomes only an enchantment, losing its Thopter type but keeping flying") {
        val (driver, thopter) = enchant("Ornithopter")
        val projected = driver.state.projectedState

        projected.hasType(thopter, "ENCHANTMENT") shouldBe true
        projected.isCreature(thopter) shouldBe false
        projected.hasType(thopter, "ARTIFACT") shouldBe false
        projected.hasSubtype(thopter, "Thopter") shouldBe false
        projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
    }

    test("an enchantment creature stays an enchantment but stops being a creature and a Spirit") {
        val (driver, eidolon) = enchant("Hateful Eidolon")
        val projected = driver.state.projectedState

        projected.hasType(eidolon, "ENCHANTMENT") shouldBe true
        projected.isCreature(eidolon) shouldBe false
        projected.hasSubtype(eidolon, "Spirit") shouldBe false
    }

    test("a legendary creature stays legendary") {
        val (driver, prospector) = enchant("Test Hasty Prospector")
        val projected = driver.state.projectedState

        projected.isLegendary(prospector) shouldBe true
        projected.isCreature(prospector) shouldBe false
        projected.hasSubtype(prospector, "Monkey") shouldBe false
        projected.hasKeyword(prospector, Keyword.HASTE) shouldBe true
    }
})
