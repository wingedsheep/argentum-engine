package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.ChainLightning
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chain Lightning — {R} Sorcery
 * "Chain Lightning deals 3 damage to any target. Then that player or that permanent's controller
 * may pay {R}{R}. If the player does, they may copy this spell and may choose a new target for
 * that copy."
 *
 * The copy cost is mana, paid by the damaged player (or the damaged permanent's controller)
 * through a mana-payment window.
 */
class ChainLightningScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChainLightning))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, active, opponent)
    }

    fun castAtOpponent(driver: GameTestDriver, active: com.wingedsheep.sdk.model.EntityId, opponent: com.wingedsheep.sdk.model.EntityId) {
        val chain = driver.putCardInHand(active, "Chain Lightning")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpell(active, chain, listOf(opponent)).outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("damaged player pays {R}{R} from their lands to copy it back at the caster") {
        val (driver, active, opponent) = setup()
        val m1 = driver.putPermanentOnBattlefield(opponent, "Mountain")
        val m2 = driver.putPermanentOnBattlefield(opponent, "Mountain")
        castAtOpponent(driver, active, opponent)

        driver.getLifeTotal(opponent) shouldBe 17
        val offer = driver.pendingDecision
        offer.shouldBeInstanceOf<YesNoDecision>()
        offer.playerId shouldBe opponent
        offer.prompt shouldContain "Pay {R}{R} to copy Chain Lightning"
        driver.submitDecision(opponent, YesNoResponse(offer.id, true))

        val window = driver.pendingDecision
        window.shouldBeInstanceOf<SelectManaSourcesDecision>()
        window.requiredCost shouldBe "{R}{R}"
        driver.submitDecision(opponent, ManaSourcesSelectedResponse(window.id, selectedSources = listOf(m1, m2)))
        driver.isTapped(m1) shouldBe true
        driver.isTapped(m2) shouldBe true

        val targetChoice = driver.pendingDecision
        targetChoice.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(opponent, CardsSelectedResponse(targetChoice.id, listOf(active)))

        // The copy resolves; the caster has no mana to keep the chain going, so it ends.
        driver.bothPass()
        driver.getLifeTotal(active) shouldBe 17
        driver.getLifeTotal(opponent) shouldBe 17
        driver.isPaused shouldBe false
    }

    test("a player who can't produce {R}{R} is not offered the copy") {
        val (driver, active, opponent) = setup()
        driver.putPermanentOnBattlefield(opponent, "Mountain")
        driver.putPermanentOnBattlefield(opponent, "Forest")
        castAtOpponent(driver, active, opponent)

        driver.getLifeTotal(opponent) shouldBe 17
        driver.isPaused shouldBe false
    }

    test("declining in the mana window makes no copy and taps nothing") {
        val (driver, active, opponent) = setup()
        val m1 = driver.putPermanentOnBattlefield(opponent, "Mountain")
        driver.putPermanentOnBattlefield(opponent, "Mountain")
        castAtOpponent(driver, active, opponent)

        driver.submitDecision(opponent, YesNoResponse(driver.pendingDecision!!.id, true))
        val window = driver.pendingDecision
        window.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitDecision(opponent, ManaSourcesSelectedResponse(window.id, declined = true))

        driver.isPaused shouldBe false
        driver.isTapped(m1) shouldBe false
        driver.getLifeTotal(active) shouldBe 20
    }

    test("the controller of a damaged creature pays with floating mana and copies at a creature") {
        val (driver, active, opponent) = setup()
        val bear = driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")
        val myBear = driver.putPermanentOnBattlefield(active, "Grizzly Bears")
        val chain = driver.putCardInHand(active, "Chain Lightning")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpell(active, chain, listOf(bear)).outcome shouldBe Outcome.Done
        driver.giveMana(opponent, Color.RED, 2)
        driver.bothPass()

        val offer = driver.pendingDecision
        offer.shouldBeInstanceOf<YesNoDecision>()
        offer.playerId shouldBe opponent
        driver.submitDecision(opponent, YesNoResponse(offer.id, true))

        // Floating {R}{R} already covers the cost — straight to the copy's target.
        val targetChoice = driver.pendingDecision
        targetChoice.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(opponent, CardsSelectedResponse(targetChoice.id, listOf(myBear)))
        driver.bothPass()

        driver.assertInGraveyard(opponent, "Grizzly Bears")
        driver.assertInGraveyard(active, "Grizzly Bears")
    }
})
