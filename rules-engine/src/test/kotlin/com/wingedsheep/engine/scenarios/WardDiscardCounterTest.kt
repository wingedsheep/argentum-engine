package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Tests for discard-cost ward: Ward—Discard a card.
 *
 * Verifies that the caster of a spell targeting a permanent with Ward—Discard
 * is prompted to discard, and that paying lets the spell resolve while declining
 * (or being unable to discard) counters it.
 */
class WardDiscardCounterTest : FunSpec({

    val discardWardedBear = card("Discard-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(KeywordAbility.Ward(WardCost.Discard()))
    }

    val randomDiscardWardedBear = card("Random-Discard-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(KeywordAbility.Ward(WardCost.Discard(count = 1, random = true)))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(discardWardedBear, randomDiscardWardedBear))
        return driver
    }

    test("intrinsic ward-discard prompts caster with yes/no decision") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Discard-Warded Bear")

        // Give the caster a spare card so they can actually pay.
        driver.putCardInHand(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bear = driver.findPermanent(opponent, "Discard-Warded Bear")!!
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe activePlayer
    }

    test("paying ward-discard lets the targeting spell resolve") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Discard-Warded Bear")

        val spare = driver.putCardInHand(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bear = driver.findPermanent(opponent, "Discard-Warded Bear")!!
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val handBefore = driver.getHand(activePlayer)
        driver.submitYesNo(activePlayer, true)

        // Yes → caster is prompted for which card to discard.
        val pickDecision = driver.pendingDecision
        pickDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pickDecision.playerId shouldBe activePlayer
        driver.submitDecision(activePlayer, CardsSelectedResponse(pickDecision.id, listOf(spare)))

        // Bolt resolves and kills the 2/2 bear.
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(opponent, "Discard-Warded Bear") shouldBe null

        // Discarded card moved from hand to graveyard.
        driver.getHand(activePlayer).contains(spare) shouldBe false
        driver.state.getZone(activePlayer, Zone.GRAVEYARD).contains(spare) shouldBe true
        // Hand shrank by exactly one (the discarded card); bolt itself left via the stack, not the hand.
        driver.getHand(activePlayer).size shouldBe (handBefore.size - 1)
    }

    test("declining ward-discard payment counters the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Discard-Warded Bear")

        driver.putCardInHand(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bear = driver.findPermanent(opponent, "Discard-Warded Bear")!!
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val handBefore = driver.getHand(activePlayer).size
        driver.submitYesNo(activePlayer, false)

        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(opponent, "Discard-Warded Bear") shouldNotBe null
        driver.getHand(activePlayer).size shouldBe handBefore
    }

    test("ward-discard counters immediately when caster cannot pay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Discard-Warded Bear")

        // Empty the caster's hand so they'll have no card to discard after casting bolt.
        val handKey = ZoneKey(activePlayer, Zone.HAND)
        driver.replaceState(driver.state.copy(zones = driver.state.zones + (handKey to emptyList())))

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bear = driver.findPermanent(opponent, "Discard-Warded Bear")!!
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        // No decision: hand was emptied to bolt itself when cast, leaving 0 cards — counter immediately.
        driver.pendingDecision shouldBe null
        driver.findPermanent(opponent, "Discard-Warded Bear") shouldNotBe null
    }

    test("random ward-discard skips card selection and discards immediately") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Random-Discard-Warded Bear")

        driver.putCardInHand(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bear = driver.findPermanent(opponent, "Random-Discard-Warded Bear")!!
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val handBefore = driver.getHand(activePlayer).size
        driver.submitYesNo(activePlayer, true)

        // No card-selection prompt for random discard.
        // The bolt now needs to resolve; we should NOT be paused on a SelectCardsDecision.
        if (driver.pendingDecision != null) {
            driver.pendingDecision shouldNotBe null
            (driver.pendingDecision !is SelectCardsDecision) shouldBe true
        }

        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(opponent, "Random-Discard-Warded Bear") shouldBe null
        driver.getHand(activePlayer).size shouldBe (handBefore - 1)
    }
    test("ward-discard card pick names the ward permanent as its source, while the caster discards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Discard-Warded Bear")
        val spare = driver.putCardInHand(activePlayer, "Mountain")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bear = driver.findPermanent(opponent, "Discard-Warded Bear")!!
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, true)

        val pickDecision = driver.pendingDecision
        pickDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // The pick is attributed to the ward permanent, not to a player.
        pickDecision.context.sourceId shouldBe bear
        pickDecision.context.sourceName shouldBe "Discard-Warded Bear"
        // ...but the discarding player is still the spell's controller, choosing from their own hand.
        pickDecision.playerId shouldBe activePlayer
        pickDecision.options.contains(spare) shouldBe true
        pickDecision.options.all { it in driver.getHand(activePlayer) } shouldBe true

        driver.submitDecision(activePlayer, CardsSelectedResponse(pickDecision.id, listOf(spare)))
        driver.state.getZone(activePlayer, Zone.GRAVEYARD).contains(spare) shouldBe true
    }
})
