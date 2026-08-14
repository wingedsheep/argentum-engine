package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWebSlingingToSpells
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * `GrantWebSlingingToSpells` — behind Amazing Spider-Man's "Each legendary spell you cast that's one
 * or more colors has web-slinging {G}{W}{U}." Pins that a battlefield static grants web-slinging to
 * matching spells in hand (routed through `WebSlinging.effectiveWebSlinging`), while a tapped
 * creature is available to return.
 */
class GrantWebSlingingToSpellsTest : FunSpec({

    // Minimal granter: "Each legendary, one-or-more-colored spell you cast has web-slinging {G}{W}{U}".
    val amazingGrant = card("Amazing Grant Test") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantWebSlingingToSpells(
                cost = ManaCost.parse("{G}{W}{U}"),
                spellFilter = GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.IsLegendary, CardPredicate.IsColored)
                ),
            )
        }
    }

    // A legendary, colored creature spell — matches the grant.
    val legendCreature = card("Test Legend Spider") {
        manaCost = "{4}{G}"
        colorIdentity = "G"
        typeLine = "Legendary Creature — Spider"
        power = 3
        toughness = 3
    }

    // A nonlegendary colored creature spell — does NOT match the grant.
    val plainCreature = card("Test Plain Spider") {
        manaCost = "{4}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Spider"
        power = 3
        toughness = 3
    }

    // A colorless legendary spell — legendary but not colored, so it fails the IsColored half of the
    // filter ("that's one or more colors"). Exercises the predicate the nonlegendary case leaves alone.
    val colorlessLegend = card("Test Colorless Relic") {
        manaCost = "{4}"
        typeLine = "Legendary Artifact"
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(amazingGrant, legendCreature, plainCreature, colorlessLegend))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun webSlingActionCardIds(driver: GameTestDriver, playerId: EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.alternativeCostType == AlternativeCostType.WEB_SLINGING }
            .map { it.cardId }

    fun setup(driver: GameTestDriver, you: EntityId, withGranter: Boolean): EntityId {
        if (withGranter) driver.putPermanentOnBattlefield(you, "Amazing Grant Test")
        // A tapped creature to return for the web-slinging cost.
        val toReturn = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.tapPermanent(toReturn)
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveMana(you, Color.WHITE, 1)
        driver.giveMana(you, Color.BLUE, 1)
        return toReturn
    }

    test("a static grants web-slinging to a legendary, colored spell you cast") {
        val (driver, you) = newGame()
        setup(driver, you, withGranter = true)
        val legend = driver.putCardInHand(you, "Test Legend Spider")

        webSlingActionCardIds(driver, you) shouldContain legend
    }

    test("a nonlegendary spell does not get granted web-slinging") {
        val (driver, you) = newGame()
        setup(driver, you, withGranter = true)
        val plain = driver.putCardInHand(you, "Test Plain Spider")

        (plain in webSlingActionCardIds(driver, you)) shouldBe false
    }

    test("a colorless legendary spell does not get granted web-slinging") {
        val (driver, you) = newGame()
        setup(driver, you, withGranter = true)
        val relic = driver.putCardInHand(you, "Test Colorless Relic")

        (relic in webSlingActionCardIds(driver, you)) shouldBe false
    }

    test("no granter → no web-slinging for a legendary spell") {
        val (driver, you) = newGame()
        setup(driver, you, withGranter = false)
        val legend = driver.putCardInHand(you, "Test Legend Spider")

        (legend in webSlingActionCardIds(driver, you)) shouldBe false
    }
})
