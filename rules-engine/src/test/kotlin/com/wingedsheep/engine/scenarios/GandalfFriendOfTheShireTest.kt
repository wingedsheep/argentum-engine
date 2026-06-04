package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GandalfFriendOfTheShire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gandalf, Friend of the Shire (LTR — {3}{U}, Legendary Creature — Avatar Wizard, 2/4):
 *   Flash
 *   You may cast sorcery spells as though they had flash.
 *   Whenever the Ring tempts you, if you chose a creature other than Gandalf as your
 *   Ring-bearer, draw a card.
 *
 * Each clause is exercised in isolation:
 *  1. Flash keyword (printed) — covered by `QuickSliverTest`; Gandalf's own line
 *     `keywords(Keyword.FLASH)` is a single-keyword wiring and not re-tested here.
 *  2. Sorcery-flash static — controller can cast a sorcery during the end step; an opponent
 *     who controls a creature spell (not a sorcery) cannot benefit.
 *  3. Ring-tempts-you draw — fires only when the chosen Ring-bearer is a creature *other*
 *     than Gandalf (CR 701.54a via [Conditions.YouChoseOtherCreatureAsRingBearer]).
 */
class GandalfFriendOfTheShireTest : FunSpec({

    val testSorcery = CardDefinition.sorcery(
        name = "Test Sorcery",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = DrawCardsEffect(1))
    )

    val testCreature = CardDefinition.creature(
        name = "Test Beast",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 2,
        toughness = 2
    )

    val ringTempter = card("Ring Tempter") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "The Ring tempts you."
        spell { effect = Effects.TheRingTemptsYou() }
    }

    val ringBear = CardDefinition.creature("Ring Bear", ManaCost.parse("{2}"), emptySet(), 2, 2)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(GandalfFriendOfTheShire, testSorcery, testCreature, ringTempter, ringBear)
        )
        return driver
    }

    fun GameTestDriver.tempt(player: EntityId, bearerId: EntityId?) {
        val cardId = putCardInHand(player, "Ring Tempter")
        castSpell(player, cardId)
        bothPass()
        val decision = pendingDecision
        if (decision is SelectCardsDecision && bearerId != null) {
            submitDecision(player, CardsSelectedResponse(decision.id, listOf(bearerId)))
        }
    }

    // 2 — static flash-for-sorceries clause.

    test("controller may cast a sorcery during the end step with Gandalf on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf, Friend of the Shire")
        val sorcery = driver.putCardInHand(p1, "Test Sorcery")
        driver.giveMana(p1, Color.RED, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    test("the static does not grant flash to a non-sorcery spell (e.g. a creature)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf, Friend of the Shire")
        val beast = driver.putCardInHand(p1, "Test Beast")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = beast, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    test("the static is controller-only: an opponent's sorcery does not gain flash") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20))
        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf, Friend of the Shire")
        val sorcery = driver.putCardInHand(p2, "Test Sorcery")
        driver.giveMana(p2, Color.RED, 2)
        driver.passPriorityUntil(Step.END)
        driver.passPriority(p1)

        val result = driver.submit(
            CastSpell(playerId = p2, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    // 3 — Ring-tempts-you draw clause.

    test("draws a card when the Ring tempts you and you pick a creature other than Gandalf") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(active, "Gandalf, Friend of the Shire")
        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear")
        val handBefore = driver.getHandSize(active)

        driver.tempt(active, bear)
        driver.bothPass() // resolve Gandalf's Ring-tempts trigger

        driver.getHandSize(active) shouldBe handBefore + 1
    }

    test("does NOT draw when you choose Gandalf as your Ring-bearer") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gandalf = driver.putPermanentOnBattlefield(active, "Gandalf, Friend of the Shire")
        driver.putCreatureOnBattlefield(active, "Ring Bear")
        val handBefore = driver.getHandSize(active)

        driver.tempt(active, gandalf)
        driver.bothPass()

        driver.getHandSize(active) shouldBe handBefore
    }
})
