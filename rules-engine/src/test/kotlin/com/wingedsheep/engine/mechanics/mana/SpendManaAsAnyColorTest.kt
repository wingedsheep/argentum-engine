package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.SpendManaAsColor
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [SpendManaAsColor] with both ends open — "Players may spend mana as though it were mana of any
 * color" (Mycosynth Lattice, CR 609.4b). Its ruling: "lets players spend even colorless mana as
 * though it had a color. However, it doesn't remove restrictions on the mana."
 *
 * Pins: colorless and off-color mana pay colored pips, floating or from untapped lands; `{C}` still
 * demands colorless; spending restrictions survive; `Player.Each` reaches the opponent and
 * `Player.You` doesn't; the permission ends with its source; and the actual mana spent is recorded.
 */
class SpendManaAsAnyColorTest : FunSpec({

    val everyone = card("Test Any Color For Everyone") {
        manaCost = "{6}"
        typeLine = "Artifact"
        staticAbility { ability = SpendManaAsColor(fromColor = null, toColor = null, players = Player.Each) }
    }
    val onlyMe = card("Test Any Color For Me") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility { ability = SpendManaAsColor(fromColor = null, toColor = null) }
    }
    val blueAsAny = card("Test Blue As Any") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility { ability = SpendManaAsColor(fromColor = Color.BLUE, toColor = null) }
    }

    val wastes = card("Test Wastes") {
        typeLine = "Land"
        activatedAbility {
            cost = AbilityCost.Tap
            manaAbility = true
            effect = Effects.AddColorlessMana(1)
        }
    }
    val redAndColorless = card("Test Red Colorless Cantrip") {
        manaCost = "{R}{C}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val hybrid = card("Test Hybrid Cantrip") {
        manaCost = "{R/G}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(everyone, onlyMe, blueAsAny, wastes, redAndColorless, hybrid))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cost(value: String) = ManaCost.parse(value)

    test("every color accepts every color and colorless") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        val map = ManaSpendingRules.colors(driver.state, driver.player1)
        for (color in Color.entries) map[color] shouldBe (Color.entries + null).toSet()
    }

    test("floating colorless mana casts a red spell, and is recorded as colorless") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveColorlessMana(driver.player1, 1)

        driver.castSpell(driver.player1, bolt, listOf(driver.player2)).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(driver.player2) shouldBe 17
    }

    test("Islands auto-pay a red spell") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        driver.putLandOnBattlefield(driver.player1, "Island")
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")

        driver.castSpell(driver.player1, bolt, listOf(driver.player2)).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(driver.player2) shouldBe 17
    }

    test("a {C} pip still needs colorless mana") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        val pool = ManaPool(blue = 1).withSpendingColors(driver.state, driver.player1)
        pool.canPay(cost("{C}")) shouldBe false
        pool.canPay(cost("{R}")) shouldBe true
        ManaPool(colorless = 1).withSpendingColors(driver.state, driver.player1).canPay(cost("{G}")) shouldBe true
    }

    test("mana is not duplicated: one colorless pays one colored pip") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        val pool = ManaPool(colorless = 1).withSpendingColors(driver.state, driver.player1)
        pool.canPay(cost("{R}{G}")) shouldBe false
        val paid = pool.payPartial(cost("{R}"))
        paid.remainingCost.isEmpty() shouldBe true
        paid.manaSpent.colorless shouldBe 1
    }

    test("spending restrictions survive the permission") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveRestrictedMana(driver.player1, null, 1, ManaRestriction.CreatureSpellsOnly)
        driver.castSpell(driver.player1, bolt, listOf(driver.player2)).error shouldNotBe null
    }

    test("Player.Each reaches the opponent; Player.You doesn't") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        driver.putLandOnBattlefield(driver.player2, "Island")
        driver.services.manaSolver.canPay(driver.state, driver.player2, cost("{R}")) shouldBe true

        val other = newDriver()
        other.putPermanentOnBattlefield(other.player1, "Test Any Color For Me")
        other.putLandOnBattlefield(other.player1, "Island")
        other.putLandOnBattlefield(other.player2, "Island")
        other.services.manaSolver.canPay(other.state, other.player1, cost("{R}")) shouldBe true
        other.services.manaSolver.canPay(other.state, other.player2, cost("{R}")) shouldBe false
    }

    test("a single from-color stays a single from-color") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Blue As Any")
        val map = ManaSpendingRules.colors(driver.state, driver.player1)
        map[Color.RED] shouldBe setOf(Color.RED, Color.BLUE)
        ManaPool(colorless = 1).withSpendingColors(driver.state, driver.player1).canPay(cost("{R}")) shouldBe false
    }

    test("the permission ends with its source") {
        val driver = newDriver()
        val source = driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        driver.putLandOnBattlefield(driver.player1, "Island")
        driver.services.manaSolver.canPay(driver.state, driver.player1, cost("{R}")) shouldBe true
        driver.moveToGraveyard(source)
        driver.services.manaSolver.canPay(driver.state, driver.player1, cost("{R}")) shouldBe false
    }

    test("a colorless land auto-pays a red spell") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        driver.putLandOnBattlefield(driver.player1, "Test Wastes")
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")

        driver.castSpell(driver.player1, bolt, listOf(driver.player2)).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(driver.player2) shouldBe 17
    }

    test("the solver sends the colorless land to {C} and the Island to {R}") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        driver.putLandOnBattlefield(driver.player1, "Test Wastes")
        driver.putLandOnBattlefield(driver.player1, "Island")
        val spell = driver.putCardInHand(driver.player1, "Test Red Colorless Cantrip")

        driver.castSpell(driver.player1, spell).error shouldBe null
    }

    test("a colorless land pays a hybrid pip") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Any Color For Everyone")
        driver.putLandOnBattlefield(driver.player1, "Test Wastes")
        val spell = driver.putCardInHand(driver.player1, "Test Hybrid Cantrip")

        driver.castSpell(driver.player1, spell).error shouldBe null
    }

    test("without the permission a colorless land pays neither") {
        val driver = newDriver()
        driver.putLandOnBattlefield(driver.player1, "Test Wastes")
        val spell = driver.putCardInHand(driver.player1, "Test Hybrid Cantrip")

        driver.castSpell(driver.player1, spell).error shouldNotBe null
    }
})
