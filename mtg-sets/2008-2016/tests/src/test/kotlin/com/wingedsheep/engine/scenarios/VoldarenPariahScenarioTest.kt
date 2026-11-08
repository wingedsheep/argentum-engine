package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.VoldarenPariah
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Voldaren Pariah // Abolisher of Bloodlines (Eldritch Moon #111).
 *
 * Front: "Sacrifice three other creatures: Transform this creature." / "Madness {B}{B}{B}"
 * Back: "When this creature transforms into Abolisher of Bloodlines, target opponent sacrifices
 * three creatures of their choice."
 *
 * Pins the three-other-creatures cost (the Pariah can't pay with itself), the back face's
 * transforms-into trigger with the opponent choosing what to sacrifice, the "as many as able"
 * ruling, and madness on a transforming double-faced card.
 */
class VoldarenPariahScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, you, driver.getOpponent(you))
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    fun faceName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun activate(driver: GameTestDriver, you: EntityId, pariah: EntityId, fodder: List<EntityId>) =
        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = pariah,
                abilityId = VoldarenPariah.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = fodder),
            )
        )

    /** Activate, let the transform resolve, and aim the transforms-into trigger at [opponent]. */
    fun transformAgainst(driver: GameTestDriver, you: EntityId, opponent: EntityId, pariah: EntityId) {
        val fodder = List(3) { driver.putCreatureOnBattlefield(you, "Grizzly Bears") }
        activate(driver, you, pariah, fodder).error shouldBe null
        fodder.forEach { driver.getGraveyard(you) shouldContain it }
        settle(driver)
        if (driver.pendingDecision is ChooseTargetsDecision) {
            driver.submitTargetSelection(you, listOf(opponent)).error shouldBe null
        }
        settle(driver)
    }

    test("sacrificing three other creatures transforms it; the opponent sacrifices three of their choice") {
        val (driver, you, opponent) = newGame()
        val pariah = driver.putCreatureOnBattlefield(you, "Voldaren Pariah")
        val theirs = List(4) { driver.putCreatureOnBattlefield(opponent, "Hill Giant") }

        transformAgainst(driver, you, opponent, pariah)

        val choice = driver.pendingDecision as? SelectCardsDecision
        choice.shouldNotBeNull()
        choice.playerId shouldBe opponent
        val kept = theirs.last()
        driver.submitCardSelection(opponent, theirs - kept).error shouldBe null
        settle(driver)

        faceName(driver, pariah) shouldBe "Abolisher of Bloodlines"
        (theirs - kept).forEach { driver.getGraveyard(opponent) shouldContain it }
        driver.findPermanent(opponent, "Hill Giant") shouldBe kept
    }

    test("an opponent with fewer than three creatures sacrifices as many as able (ruling)") {
        val (driver, you, opponent) = newGame()
        val pariah = driver.putCreatureOnBattlefield(you, "Voldaren Pariah")
        val theirs = List(2) { driver.putCreatureOnBattlefield(opponent, "Hill Giant") }

        transformAgainst(driver, you, opponent, pariah)

        faceName(driver, pariah) shouldBe "Abolisher of Bloodlines"
        theirs.forEach { driver.getGraveyard(opponent) shouldContain it }
    }

    test("the Pariah can't be one of the three creatures it sacrifices") {
        val (driver, you, _) = newGame()
        val pariah = driver.putCreatureOnBattlefield(you, "Voldaren Pariah")
        val fodder = List(2) { driver.putCreatureOnBattlefield(you, "Grizzly Bears") }

        withClue("only two other creatures, so the cost can't be paid") {
            activate(driver, you, pariah, fodder + pariah).error shouldNotBe null
        }
        faceName(driver, pariah) shouldBe "Voldaren Pariah"
        fodder.forEach { driver.getGraveyard(you) shouldNotContain it }
    }

    test("discarded, it is exiled and can be cast for its madness cost of {B}{B}{B}, front face up") {
        val (driver, you, _) = newGame()
        val pariah = driver.putCardInHand(you, "Voldaren Pariah")

        val voice = driver.putCardInHand(you, "Tormenting Voice")
        driver.giveMana(you, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = you,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(pariah)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.getExile(you) shouldContain pariah
        driver.getGraveyard(you) shouldNotContain pariah

        driver.giveMana(you, Color.BLACK, 3)
        settle(driver)
        driver.submitYesNo(you, true)
        settle(driver)

        val permanent = driver.findPermanent(you, "Voldaren Pariah")
        permanent.shouldNotBeNull()
        faceName(driver, permanent) shouldBe "Voldaren Pariah"
    }
})
