package com.wingedsheep.engine.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phyrexian mana in **activated-ability** costs (CR 107.4f: a Phyrexian symbol can be paid with
 * one mana of its color or with 2 life) — the transform cost of March of the Machine's
 * Phyrexian DFCs ("{4}{R/P}: Transform this creature").
 *
 * The cast path always honoured `PaymentStrategy.Explicit.phyrexianLifePayments`; activation
 * ignored it, and neither auto-pay path ever spent life — yet `ManaSolver.canPay` counted life as
 * payment, so an ability affordable only through life was offered and then failed to pay. Both
 * payers now pay the player's explicit split, and on auto-pay the fewest life pips that leave the
 * rest payable with mana (`ManaSolver.choosePhyrexianLifePayments`).
 */
class PhyrexianActivationCostTest : FunSpec({

    val siphon = card("Phyrexian Test Siphon") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{1}{R/P}: Draw a card."
        activatedAbility {
            cost = Costs.Mana("{1}{R/P}")
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }

    val doubleSiphon = card("Phyrexian Test Double Siphon") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{R/P}{R/P}: Draw a card."
        activatedAbility {
            cost = Costs.Mana("{R/P}{R/P}")
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }

    val probe = card("Phyrexian Test Probe") {
        manaCost = "{R/P}"
        typeLine = "Sorcery"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(siphon, doubleSiphon, probe))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to me
    }

    fun GameTestDriver.abilityId(name: String) = cardRegistry.requireCard(name).activatedAbilities[0].id

    fun GameTestDriver.isAffordable(player: EntityId, sourceId: EntityId) =
        legalActions(player).any { it.affordable && (it.action as? ActivateAbility)?.sourceId == sourceId }

    test("auto-pay spends 2 life for a Phyrexian pip no source can make") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Phyrexian Test Siphon")
        val island = driver.putLandOnBattlefield(me, "Island")
        val hand = driver.getHandSize(me)

        withClue("affordable only through life") { driver.isAffordable(me, source) shouldBe true }
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = source, abilityId = driver.abilityId("Phyrexian Test Siphon")))

        driver.getLifeTotal(me) shouldBe 18
        driver.isTapped(island) shouldBe true
        driver.bothPass()
        driver.getHandSize(me) shouldBe hand + 1
    }

    test("auto-pay pays the pip with mana when a source of its color is available") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Phyrexian Test Siphon")
        val island = driver.putLandOnBattlefield(me, "Island")
        val mountain = driver.putLandOnBattlefield(me, "Mountain")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = source, abilityId = driver.abilityId("Phyrexian Test Siphon")))

        withClue("life is never spent when mana covers the pip") { driver.getLifeTotal(me) shouldBe 20 }
        driver.isTapped(island) shouldBe true
        driver.isTapped(mountain) shouldBe true
    }

    test("an explicit life payment is honoured even when red mana is available") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Phyrexian Test Siphon")
        val island = driver.putLandOnBattlefield(me, "Island")
        val mountain = driver.putLandOnBattlefield(me, "Mountain")

        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = source, abilityId = driver.abilityId("Phyrexian Test Siphon"),
                paymentStrategy = PaymentStrategy.Explicit(
                    manaAbilitiesToActivate = listOf(island),
                    phyrexianLifePayments = listOf(Color.RED)
                )
            )
        )

        driver.getLifeTotal(me) shouldBe 18
        driver.isTapped(island) shouldBe true
        withClue("the Mountain stays untapped — the pip was paid with life") { driver.isTapped(mountain) shouldBe false }
    }

    test("auto-pay spends life on only the pips mana can't cover") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Phyrexian Test Double Siphon")
        val mountain = driver.putLandOnBattlefield(me, "Mountain")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = source, abilityId = driver.abilityId("Phyrexian Test Double Siphon")))

        withClue("one pip from the Mountain, one from 2 life") {
            driver.isTapped(mountain) shouldBe true
            driver.getLifeTotal(me) shouldBe 18
        }
    }

    test("a player can't pay more life than they have (CR 119.4)") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Phyrexian Test Siphon")
        val island = driver.putLandOnBattlefield(me, "Island")
        driver.setLifeTotal(me, 1)

        withClue("1 life and no red source — not affordable") { driver.isAffordable(me, source) shouldBe false }
        driver.submitExpectFailure(ActivateAbility(playerId = me, sourceId = source, abilityId = driver.abilityId("Phyrexian Test Siphon")))
        driver.submit(
            ActivateAbility(
                playerId = me, sourceId = source, abilityId = driver.abilityId("Phyrexian Test Siphon"),
                paymentStrategy = PaymentStrategy.Explicit(listOf(island), phyrexianLifePayments = listOf(Color.RED))
            )
        ).error shouldNotBe null
        driver.getLifeTotal(me) shouldBe 1
        driver.isTapped(island) shouldBe false
    }

    test("paying down to exactly 0 life is allowed") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Phyrexian Test Siphon")
        driver.putLandOnBattlefield(me, "Island")
        driver.setLifeTotal(me, 2)

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = source, abilityId = driver.abilityId("Phyrexian Test Siphon")))
        driver.getLifeTotal(me) shouldBe 0
    }

    test("casting on auto-pay also spends life for a pip no source can make") {
        val (driver, me) = newDriver()
        val spell = driver.putCardInHand(me, "Phyrexian Test Probe")
        val hand = driver.getHandSize(me)

        driver.submitSuccess(CastSpell(playerId = me, cardId = spell))

        driver.getLifeTotal(me) shouldBe 18
        driver.bothPass()
        withClue("Probe left the hand and drew a card") { driver.getHandSize(me) shouldBe hand }
    }
})
