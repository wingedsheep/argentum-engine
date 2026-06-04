package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.FlashGrantsThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.BorneUponAWind
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Borne Upon a Wind ({1}{U} Instant): "You may cast spells this turn as though they had
 * flash. Draw a card."
 *
 * The flash-permission primitive (GrantFlashToSpellsEffect / FlashGrantsThisTurnComponent)
 * has its own unit coverage in [GrantFlashToSpellsEffectTest]. This test pins the printed
 * card text end-to-end:
 *  * resolving Borne Upon a Wind grants flash AND draws exactly one card
 *  * the controller can cast a sorcery during the end step of the same turn (CR 702.8a, 601.3)
 *  * the grant clears at the cleanup step (CR 514.2), so the next turn the same sorcery
 *    can no longer be cast at instant speed
 */
class BorneUponAWindTest : FunSpec({

    val testSorcery = CardDefinition.sorcery(
        name = "Test Sorcery",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = DrawCardsEffect(1))
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BorneUponAWind, testSorcery))
        return driver
    }

    test("resolving Borne Upon a Wind grants flash and draws exactly one card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(p1)
        val spell = driver.putCardInHand(p1, "Borne Upon a Wind")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.submitSuccess(
            CastSpell(playerId = p1, cardId = spell, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        // The "Draw a card" half resolved: putCardInHand bumped the hand by 1, but the cast
        // moved it back out, leaving the net change = +1 from the draw.
        driver.getHandSize(p1) shouldBe handBefore + 1

        val grants = driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>()
        grants.shouldNotBeNull()
        grants.filters.size shouldBe 1
    }

    test("the controller may cast a sorcery during the end step the same turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val borne = driver.putCardInHand(p1, "Borne Upon a Wind")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.submitSuccess(
            CastSpell(playerId = p1, cardId = borne, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        val sorcery = driver.putCardInHand(p1, "Test Sorcery")
        driver.giveMana(p1, Color.RED, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    test("the flash grant does not survive into the next turn (CR 514.2)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20))
        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val borne = driver.putCardInHand(p1, "Borne Upon a Wind")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.submitSuccess(
            CastSpell(playerId = p1, cardId = borne, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()
        driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>().shouldNotBeNull()

        // Advance past p1's CLEANUP into p2's upkeep.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>().shouldBeNull()

        val sorcery = driver.putCardInHand(p1, "Test Sorcery")
        driver.giveMana(p1, Color.RED, 2)
        driver.passPriorityUntil(Step.END)
        driver.passPriority(p2) // priority on p1 during p2's end step

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }
})
