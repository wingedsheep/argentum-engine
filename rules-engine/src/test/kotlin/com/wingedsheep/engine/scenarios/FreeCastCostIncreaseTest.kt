package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A spell cast "without paying its mana cost" still owes the cost increases (CR 601.2f).
 *
 * That wording is an alternative cost of {0} (CR 118.9): it replaces the mana cost only, so the
 * total cost is {0} plus every increase, minus every reduction. A Sphere of Resistance or Thalia
 * makes a cascaded, discovered or Weftwalked spell cost {1}; a matching reduction takes it back
 * off. Before this, free casts were priced at a flat {0} and skipped the modifier pipeline.
 */
class FreeCastCostIncreaseTest : FunSpec({

    // Sphere of Resistance: "Spells cost {1} more to cast."
    val taxSphere = card("Test Tax Sphere") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.AnyCaster(GameObjectFilter.Any),
                modification = CostModification.IncreaseGeneric(1),
            )
        }
    }

    // "Spells you cast cost {1} less to cast."
    val discountEngine = card("Test Discount Engine") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGeneric(1),
            )
        }
    }

    // A Weftwalking-style "you may cast spells without paying their mana costs".
    val freeCaster = card("Test Free Caster") {
        manaCost = "{3}{U}"
        typeLine = "Enchantment"
        staticAbility {
            ability = MayCastWithoutPayingManaCost(controllerOnly = true)
        }
    }

    // A server-initiated free cast: discover exiles Grizzly Bears and offers to cast it for free.
    val discoverRitual = card("Test Discover Ritual") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Discover(3)
        }
    }

    // An improvise sorcery: tapped artifacts pay generic mana in its total cost (CR 702.126a).
    val improviseRitual = card("Test Improvise Ritual") {
        manaCost = "{3}{U}"
        typeLine = "Sorcery"
        keywords(Keyword.IMPROVISE)
        spell {
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(taxSphere, discountEngine, freeCaster, discoverRitual, improviseRitual))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.freeCastOffer(player: EntityId, cardId: EntityId) = legalActions(player).firstOrNull {
        val action = it.action
        action is CastSpell && action.cardId == cardId && action.useWithoutPayingManaCost
    }

    test("with no cost modifiers a free cast is offered for {0}") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Test Free Caster")
        val bears = driver.putCardInHand(player, "Grizzly Bears")

        val offer = driver.freeCastOffer(player, bears)
        offer shouldNotBe null
        offer!!.manaCostString shouldBe "{0}"
        driver.submit(
            CastSpell(player, bears, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
    }

    test("a battlefield cost increase applies to a free cast, and is paid with mana") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Test Free Caster")
        driver.putPermanentOnBattlefield(player, "Test Tax Sphere")
        val forest = driver.putLandOnBattlefield(player, "Forest")
        val bears = driver.putCardInHand(player, "Grizzly Bears")

        val offer = driver.freeCastOffer(player, bears)
        withClue("the free variant is priced at the tax, not {0}") {
            offer shouldNotBe null
            offer!!.manaCostString shouldBe "{1}"
        }
        driver.submit(CastSpell(player, bears, useWithoutPayingManaCost = true)).outcome shouldBe Outcome.Done
        withClue("the Forest paid the {1} tax") { driver.isTapped(forest) shouldBe true }
        driver.bothPass()
        driver.findPermanent(player, "Grizzly Bears") shouldNotBe null
    }

    test("a taxed free cast the player can't pay isn't offered and is rejected") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Test Free Caster")
        driver.putPermanentOnBattlefield(player, "Test Tax Sphere")
        val bears = driver.putCardInHand(player, "Grizzly Bears")

        driver.freeCastOffer(player, bears) shouldBe null
        val result = driver.submit(
            CastSpell(player, bears, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.error shouldNotBe null
    }

    test("reductions apply after increases on a free cast, so they can cancel the tax") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Test Free Caster")
        driver.putPermanentOnBattlefield(player, "Test Tax Sphere")
        driver.putPermanentOnBattlefield(player, "Test Discount Engine")
        val bears = driver.putCardInHand(player, "Grizzly Bears")

        driver.freeCastOffer(player, bears)!!.manaCostString shouldBe "{0}"
        driver.submit(
            CastSpell(player, bears, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
    }

    test("a discovered spell cast for free pays the tax with available mana") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Test Tax Sphere")
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val forest = driver.putLandOnBattlefield(player, "Forest")
        driver.giveMana(player, Color.GREEN, 2) // the ritual's {G} plus the Sphere's {1}
        val ritual = driver.putCardInHand(player, "Test Discover Ritual")
        driver.castSpell(player, ritual).outcome shouldBe Outcome.Done
        driver.bothPass()

        val yesNo = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(player, YesNoResponse(yesNo.id, choice = true)).error shouldBe null
        withClue("the Forest paid the free cast's {1} tax") { driver.isTapped(forest) shouldBe true }
        driver.bothPass()
        driver.findPermanent(player, "Grizzly Bears") shouldNotBe null
    }

    test("a discovered spell whose tax can't be paid isn't cast and goes to hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Test Tax Sphere")
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        driver.giveMana(player, Color.GREEN, 2)
        val ritual = driver.putCardInHand(player, "Test Discover Ritual")
        driver.castSpell(player, ritual).outcome shouldBe Outcome.Done
        driver.bothPass()

        val yesNo = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(player, YesNoResponse(yesNo.id, choice = true)).error shouldBe null
        driver.findPermanent(player, "Grizzly Bears") shouldBe null
        driver.getHand(player).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("improvise can tap an artifact to pay a free cast's tax") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Test Free Caster")
        val sphere = driver.putPermanentOnBattlefield(player, "Test Tax Sphere")
        val ritual = driver.putCardInHand(player, "Test Improvise Ritual")

        driver.submit(
            CastSpell(
                player, ritual, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = setOf(sphere)),
            )
        ).outcome shouldBe Outcome.Done
        withClue("the tapped Sphere paid its own {1} tax") { driver.isTapped(sphere) shouldBe true }
    }
})
