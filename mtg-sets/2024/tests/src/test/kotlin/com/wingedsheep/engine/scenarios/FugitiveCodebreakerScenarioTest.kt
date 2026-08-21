package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.FugitiveCodebreaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fugitive Codebreaker — "Disguise {5}{R}. This cost is reduced by {1} for each instant and sorcery
 * card in your graveyard."
 *
 * The reduction is the first one to ride a *turn-up procedure* rather than a `ModifySpellCost`
 * static, so what these tests pin down is that the new
 * [com.wingedsheep.sdk.scripting.KeywordAbility.Disguise.costReduction] actually reaches the price
 * at all three sites that quote it — the enumerated legal action, the action's validation, and the
 * payment — and that it obeys the ordinary rules of a cost reduction while it's there:
 *
 * - only instants and sorceries count, not every card in the yard;
 * - it eats generic mana only, so the {R} is a floor no graveyard can get under (CR 202.2a);
 * - it is re-read at every price check, so a card that hits the graveyard between two checks moves
 *   the price rather than locking in whatever it was when the permanent came down.
 *
 * The last test covers the card's other half: the refill is a turned-face-up trigger, so it fires
 * on the flip and not on a hard cast.
 */
class FugitiveCodebreakerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FugitiveCodebreaker)
        return driver
    }

    /** Put the Codebreaker onto the battlefield face down under disguise, as a real cast would. */
    fun GameTestDriver.putDisguised(playerId: EntityId): EntityId {
        val id = putCreatureOnBattlefield(playerId, "Fugitive Codebreaker")
        val cardDef = cardRegistry.requireCard("Fugitive Codebreaker")
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent).with(FaceDownModeComponent(FaceDownMode.DISGUISE))
                FaceDownTurnUp.dataFor(cardDef, "Fugitive Codebreaker", FaceDownMode.DISGUISE)
                    ?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    /**
     * The mana cost the turn-face-up legal action quotes for [permanentId].
     *
     * The enumerator only offers a turn-up the player can actually afford, so this floats the pool
     * well above any price under test first — the question here is what is *quoted*, which is a
     * separate matter from what is affordable (the tests that care about affordability set the pool
     * exactly and submit the action).
     */
    fun GameTestDriver.quotedTurnUpCost(playerId: EntityId, permanentId: EntityId): String? {
        giveMana(playerId, Color.RED, 2)
        giveColorlessMana(playerId, 10)
        return legalActions(playerId)
            .firstOrNull { (it.action as? TurnFaceUp)?.sourceId == permanentId }
            ?.manaCostString
    }

    test("an empty graveyard leaves the printed {5}{R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)

        withClue("nothing in the graveyard, so the action quotes the printed disguise cost") {
            driver.quotedTurnUpCost(you, codebreaker) shouldBe "{5}{R}"
        }
    }

    test("with an empty graveyard, five mana can't pay the unreduced flip") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        driver.giveMana(you, Color.RED, 1)
        driver.giveColorlessMana(you, 4)

        driver.submitExpectFailure(
            TurnFaceUp(playerId = you, sourceId = codebreaker, paymentStrategy = PaymentStrategy.FromPool)
        )
        withClue("the flip was rejected, so it is still face down") {
            driver.state.getEntity(codebreaker)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        }
    }

    test("each instant and sorcery in the graveyard shaves {1} off the flip") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        driver.putCardInGraveyard(you, "Lightning Bolt")
        driver.putCardInGraveyard(you, "Giant Growth")
        driver.putCardInGraveyard(you, "Careful Study")

        withClue("two instants and a sorcery — {5}{R} minus {3}") {
            driver.quotedTurnUpCost(you, codebreaker) shouldBe "{2}{R}"
        }
    }

    test("the reduction reaches the payment, not just the quote") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        driver.putCardInGraveyard(you, "Lightning Bolt")
        driver.putCardInGraveyard(you, "Giant Growth")
        driver.putCardInGraveyard(you, "Careful Study")

        // Exactly {2}{R} in the pool and not a mana more.
        driver.giveMana(you, Color.RED, 1)
        driver.giveColorlessMana(you, 2)
        driver.submitSuccess(
            TurnFaceUp(playerId = you, sourceId = codebreaker, paymentStrategy = PaymentStrategy.FromPool)
        )

        withClue("three mana paid a six-mana printed cost, so the reduction reached the payment too") {
            driver.state.getEntity(codebreaker)?.get<FaceDownComponent>() shouldBe null
        }
    }

    test("only instants and sorceries count — a graveyard of creatures changes nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        driver.putCardInGraveyard(you, "Centaur Courser")
        driver.putCardInGraveyard(you, "Savannah Lions")
        driver.putCardInGraveyard(you, "Test Enchantment")

        driver.quotedTurnUpCost(you, codebreaker) shouldBe "{5}{R}"
    }

    test("the opponent's instants don't help — the reduction reads your graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        repeat(4) { driver.putCardInGraveyard(opponent, "Lightning Bolt") }

        driver.quotedTurnUpCost(you, codebreaker) shouldBe "{5}{R}"
    }

    test("the reduction is generic-only — a stuffed graveyard still leaves the {R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        repeat(9) { driver.putCardInGraveyard(you, "Lightning Bolt") }

        withClue("nine instants is more than the {5} generic, and the {R} survives (CR 202.2a)") {
            driver.quotedTurnUpCost(you, codebreaker) shouldBe "{R}"
        }
    }

    test("the surviving {R} is a real floor — colorless alone can't pay it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        repeat(9) { driver.putCardInGraveyard(you, "Lightning Bolt") }

        driver.giveColorlessMana(you, 5)
        driver.submitExpectFailure(
            TurnFaceUp(playerId = you, sourceId = codebreaker, paymentStrategy = PaymentStrategy.FromPool)
        )
        withClue("colorless can't pay the {R}, however cheap the flip got") {
            driver.state.getEntity(codebreaker)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        }

        driver.giveMana(you, Color.RED, 1)
        driver.submitSuccess(
            TurnFaceUp(playerId = you, sourceId = codebreaker, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.state.getEntity(codebreaker)?.get<FaceDownComponent>() shouldBe null
    }

    test("the price is re-read, not locked in when the permanent came down") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        driver.quotedTurnUpCost(you, codebreaker) shouldBe "{5}{R}"

        driver.putCardInGraveyard(you, "Counterspell")
        driver.putCardInGraveyard(you, "Doom Blade")

        withClue("two instants arrived after the permanent did, and the price moved with them") {
            driver.quotedTurnUpCost(you, codebreaker) shouldBe "{3}{R}"
        }
    }

    test("flipping it face up discards the hand, then draws three") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val codebreaker = driver.putDisguised(you)
        repeat(5) { driver.putCardInGraveyard(you, "Lightning Bolt") }
        driver.putCardInHand(you, "Giant Growth")
        driver.putCardInHand(you, "Doom Blade")
        val handBefore = driver.getHand(you).size

        driver.giveMana(you, Color.RED, 1)
        driver.submitSuccess(
            TurnFaceUp(playerId = you, sourceId = codebreaker, paymentStrategy = PaymentStrategy.FromPool)
        )

        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        withClue("whatever the hand held ($handBefore cards), exactly three cards replaced it") {
            driver.getHand(you).size shouldBe 3
        }
        withClue("the three drawn cards are not themselves discarded — discard resolves first") {
            driver.getHand(you).all { driver.getCardName(it) == "Mountain" } shouldBe true
        }
    }

    test("a hard cast gets no refill — the trigger is turned-face-up, not enters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cardId = driver.putCardInHand(you, "Fugitive Codebreaker")
        driver.putCardInHand(you, "Giant Growth")
        driver.giveMana(you, Color.RED, 1)
        driver.giveColorlessMana(you, 1)
        val handBefore = driver.getHand(you).size

        // Hard cast for {1}{R} — the real cast path, so an enters-trigger miswiring would fire here.
        driver.castSpell(you, cardId).error shouldBe null
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.assertPermanentExists(you, "Fugitive Codebreaker")
        withClue("it entered face up, nothing was turned face up, so only the cast left the hand") {
            driver.getHand(you).size shouldBe handBefore - 1
        }
    }

    test("the card definition carries the disguise cost and its reduction") {
        val disguise = FugitiveCodebreaker.keywordAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Disguise>()
            .single()
        disguise.disguiseCost.description shouldBe "{5}{R}"
        disguise.costReduction shouldBe com.wingedsheep.sdk.scripting.CostReductionSource
            .CardsInGraveyardMatchingFilter(
                filter = com.wingedsheep.sdk.scripting.GameObjectFilter.InstantOrSorcery,
                amountPerCard = 1
            )
    }
})
