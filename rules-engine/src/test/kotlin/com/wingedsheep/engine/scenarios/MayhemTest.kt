package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.conditions.MayhemCostWasPaid
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for the Mayhem [cost] keyword (CR 702.187, Marvel's Spider-Man).
 *
 * "Mayhem [cost]" — *"As long as you discarded this card this turn, you may cast it from your
 * graveyard by paying [cost] rather than paying its mana cost."* (CR 702.187b) It grants no timing
 * permission (normal timing), and — unlike Flashback/Harmonize — the spell is NOT exiled on
 * resolution: a permanent enters the battlefield, an instant/sorcery goes to the graveyard as
 * normal. A rider can read that the mayhem cost was paid (CR 702.187 links it to the card's own
 * ability).
 *
 * Exercised with inline cards so the engine behavior is pinned independent of the SPM set.
 */
class MayhemTest : FunSpec({

    // A vanilla mayhem creature ({3}{B}, Mayhem {B}) — non-Flash, so sorcery-speed only.
    val mayhemBeast = card("Mayhem Beast") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
        mayhem("{B}")
    }

    // A mayhem creature with Flash — castable via mayhem any time you have priority.
    val flashMayhem = card("Flash Mayhem") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        keywords(com.wingedsheep.sdk.core.Keyword.FLASH)
        mayhem("{B}")
    }

    // A mayhem SORCERY whose effect branches on whether the mayhem cost was paid (mirrors
    // Sandman's Quicksand). Gain 5 life if mayhem-cast, else 2 — easy to assert, and proves the
    // spell returns to the graveyard (not exile) after a mayhem cast.
    val mayhemBolt = card("Mayhem Bolt") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery"
        spell {
            effect = ConditionalEffect(
                condition = Conditions.MayhemCostWasPaid,
                effect = Effects.GainLife(5),
                elseEffect = Effects.GainLife(2)
            )
        }
        mayhem("{R}")
    }

    // A plain "you discard a card" sorcery used to drive a real discard (exercises trackDiscard).
    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Discard(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(mayhemBeast, flashMayhem, mayhemBolt, selfDiscard)
        )
        return driver
    }

    fun markDiscarded(driver: GameTestDriver, playerId: com.wingedsheep.sdk.model.EntityId, cardId: com.wingedsheep.sdk.model.EntityId) {
        val prior = driver.state.getEntity(playerId)
            ?.get<CardsDiscardedThisTurnComponent>() ?: CardsDiscardedThisTurnComponent()
        driver.addComponent(
            playerId,
            prior.copy(cardIds = prior.cardIds + cardId, count = prior.count + 1)
        )
    }

    fun mayhemActions(driver: GameTestDriver, playerId: com.wingedsheep.sdk.model.EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.alternativeCostType == AlternativeCostType.MAYHEM }

    test("Mayhem is offered only if you discarded the card this turn (CR 702.187b)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCardInGraveyard(player, "Mayhem Beast")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 1)

        // In the graveyard but NOT discarded this turn: no mayhem action (unlike flashback).
        mayhemActions(driver, player).isEmpty().shouldBeTrue()

        // Mark it discarded this turn: the mayhem cast now appears.
        markDiscarded(driver, player, beast)
        mayhemActions(driver, player).map { it.cardId } shouldContain beast
    }

    test("Mayhem-cast permanent ENTERS the battlefield (not exiled) and carries the mayhem-paid flag") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCardInGraveyard(player, "Mayhem Beast")
        markDiscarded(driver, player, beast)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 1)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = beast,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.MAYHEM,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${result.error} pending=${result.pendingDecision}") {
            result.isSuccess shouldBe true
        }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // It entered the battlefield — NOT exiled (the key difference from Flashback/Harmonize).
        val perm = driver.findPermanent(player, "Mayhem Beast")
        perm.shouldNotBeNull()
        driver.getExile(player).shouldNotContain(beast)

        // Durable mayhem-paid flag + condition.
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.MAYHEM_CAST) shouldBe true
        ConditionEvaluator().evaluate(
            driver.state,
            MayhemCostWasPaid,
            EffectContext(sourceId = perm, controllerId = player)
        ).shouldBeTrue()
    }

    test("Mayhem-cast SORCERY resolves its mayhem branch and returns to the graveyard (not exile)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bolt = driver.putCardInGraveyard(player, "Mayhem Bolt")
        markDiscarded(driver, player, bolt)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 1)
        val lifeBefore = driver.getLifeTotal(player)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = bolt,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.MAYHEM,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Mayhem branch ran (gain 5, not 2).
        driver.getLifeTotal(player) shouldBe lifeBefore + 5
        // The sorcery went to the graveyard, NOT exile.
        driver.getGraveyard(player) shouldContain bolt
        driver.getExile(player).shouldNotContain(bolt)
    }

    test("a Mayhem spell that resolves back to the graveyard cannot be Mayhem-cast again (CR 400.7)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bolt = driver.putCardInGraveyard(player, "Mayhem Bolt")
        markDiscarded(driver, player, bolt)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 2)

        // First Mayhem cast resolves and the sorcery returns to the graveyard.
        driver.submit(
            CastSpell(
                playerId = player, cardId = bolt,
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.MAYHEM,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // It's back in the graveyard, but is a new object (CR 400.7) that you did NOT discard —
        // so no second Mayhem cast is offered even though it sits there with mana available.
        driver.getGraveyard(player) shouldContain bolt
        driver.giveMana(player, Color.RED, 2)
        mayhemActions(driver, player).map { it.cardId } shouldNotContain bolt

        // The monotonic "discarded this turn" count is unchanged by the recast attempt.
        driver.state.getEntity(player)?.get<CardsDiscardedThisTurnComponent>()?.count shouldBe 1
    }

    test("casting the sorcery normally from hand does NOT trigger the mayhem-paid branch") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bolt = driver.putCardInHand(player, "Mayhem Bolt")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 3)
        val lifeBefore = driver.getLifeTotal(player)

        val result = driver.submit(
            CastSpell(playerId = player, cardId = bolt, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(player) shouldBe lifeBefore + 2
    }

    test("Mayhem grants no timing permission: a non-Flash creature is not offered at instant speed, a Flash one is") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCardInGraveyard(player, "Mayhem Beast")
        val flash = driver.putCardInGraveyard(player, "Flash Mayhem")
        markDiscarded(driver, player, beast)
        markDiscarded(driver, player, flash)
        driver.giveMana(player, Color.BLACK, 2)

        // At an instant-speed window (end step, empty stack), only the Flash creature is castable.
        driver.passPriorityUntil(Step.END)
        val ids = mayhemActions(driver, player).map { it.cardId }
        ids shouldContain flash
        ids shouldNotContain beast
    }

    test("trackDiscard: actually discarding a card records it and makes Mayhem legal") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCardInHand(player, "Mayhem Beast")
        val outlet = driver.putCardInHand(player, "Self Discard")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 3) // {1} for outlet + {B} for the mayhem cast

        // Cast the discard outlet, then resolve it — its Discard(1) prompts a card selection, so
        // choose the mayhem card to discard.
        val cast = driver.submit(
            CastSpell(playerId = player, cardId = outlet, paymentStrategy = PaymentStrategy.FromPool)
        )
        cast.isSuccess shouldBe true
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            val pending = driver.pendingDecision
            if (pending is SelectCardsDecision) {
                driver.submitCardSelection(player, listOf(beast))
            } else if (driver.state.stack.isNotEmpty()) {
                driver.bothPass()
            } else break
        }

        // The discard hook recorded the card, and it's now in the graveyard.
        driver.getGraveyard(player) shouldContain beast
        driver.state.getEntity(player)?.get<CardsDiscardedThisTurnComponent>()?.cardIds?.contains(beast) shouldBe true
        // ...so Mayhem is now a legal action for it.
        mayhemActions(driver, player).map { it.cardId } shouldContain beast
    }

    test("discarded-this-turn is cleared at the start of the next turn (TurnManager reset)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCardInGraveyard(player, "Mayhem Beast")
        markDiscarded(driver, player, beast)
        driver.state.getEntity(player)?.get<CardsDiscardedThisTurnComponent>()?.cardIds?.contains(beast) shouldBe true

        // Advance into the next turn; the per-turn reset empties every player's discard list.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.getEntity(player)?.get<CardsDiscardedThisTurnComponent>()?.cardIds?.contains(beast).let {
            (it ?: false).shouldBeFalse()
        }
    }
})
