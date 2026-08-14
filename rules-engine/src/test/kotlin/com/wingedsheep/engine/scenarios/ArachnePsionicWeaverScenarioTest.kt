package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.spm.cards.ArachnePsionicWeaver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Arachne, Psionic Weaver (SPM) — "As Arachne enters, look at an opponent's hand, then choose a card
 * type other than creature. Spells of the chosen type cost {1} more to cast."
 *
 * Pins the new durable card-type choice ([Effects.ChooseCardTypeForSource] → [ChoiceSlot.CARD_TYPE])
 * and the cost tax keyed to it ([CardPredicate.CardTypeEqualsChosenComponent] read at
 * cost-calculation time).
 */
class ArachnePsionicWeaverScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArachnePsionicWeaver))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    /** Cast Arachne and lock in [type] as its chosen card type. Returns Arachne's entity id. */
    fun castArachneChoosing(driver: GameTestDriver, you: EntityId, type: String): EntityId {
        driver.giveMana(you, Color.WHITE, 1)
        driver.giveColorlessMana(you, 2)
        val arachne = driver.putCardInHand(you, "Arachne, Psionic Weaver")
        driver.castSpell(you, arachne)
        settle(driver) // Arachne resolves, ETB trigger resolves, pauses on the card-type choice
        val pick = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(you, OptionChosenResponse(pick.id, pick.options.indexOf(type)))
        settle(driver)
        return arachne
    }

    test("the chosen card type is written durably onto Arachne") {
        val (driver, you, opponent) = newGame()
        driver.putCardInHand(opponent, "Lightning Bolt") // so the hand-look has content
        val arachne = castArachneChoosing(driver, you, "Instant")

        val chosen = driver.state.getEntity(arachne)
            ?.get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.CARD_TYPE)
        (chosen as? ChoiceValue.TextChoice)?.text shouldBe "Instant"
    }

    test("the chosen card type is surfaced to the client view") {
        val (driver, you, opponent) = newGame()
        driver.putCardInHand(opponent, "Lightning Bolt")
        val arachne = castArachneChoosing(driver, you, "Instant")

        // The durable card-type choice is visible to the client (rendered as a badge), not just
        // stored on the permanent's CastChoicesComponent.
        val view = ClientStateTransformer(cardRegistry = driver.cardRegistry)
            .transform(driver.state, viewingPlayerId = you)
        view.cards[arachne]?.chosenCardType shouldBe "Instant"
    }

    test("spells of the chosen type cost {1} more; other types are untaxed") {
        val (driver, you, opponent) = newGame()
        castArachneChoosing(driver, you, "Instant")

        // Lightning Bolt is an instant ({R}); with the tax it costs {1}{R}.
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1) // only {R} — not enough for the taxed {1}{R}
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldNotBe null

        driver.giveColorlessMana(you, 1) // now {1}{R} available
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        settle(driver) // resolve the bolt so the stack is empty for the sorcery-speed control cast

        // A sorcery is not the chosen type, so it is untaxed: Careful Study ({B}) pays exactly {B}.
        val study = driver.putCardInHand(you, "Careful Study")
        driver.giveMana(you, Color.BLACK, 1)
        driver.castSpell(you, study).error shouldBe null
    }
})
