package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Chain of Smog.
 *
 * Chain of Smog: {1}{B}
 * Sorcery
 * Target player discards two cards. That player may copy this spell and may choose
 * a new target for that copy.
 */
class ChainOfSmogTest : FunSpec({

    val ChainOfSmog = CardDefinition.sorcery(
        name = "Chain of Smog",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "Target player discards two cards. That player may copy this spell and may choose a new target for that copy.",
        script = CardScript.spell(
            effect = Effects.DiscardAndChainCopy(
                count = 2,
                target = EffectTarget.BoundVariable("target"),
                spellName = "Chain of Smog"
            ),
            TargetPlayer(id = "target")
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChainOfSmog))
        return driver
    }

    test("Chain of Smog makes target player choose and discard two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val opponentHandBefore = driver.getHandSize(opponent)

        val chain = driver.putCardInHand(activePlayer, "Chain of Smog")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Opponent must choose 2 cards to discard (hand is > 2)
        driver.isPaused shouldBe true
        val discardDecision = driver.pendingDecision
        discardDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discardDecision.minSelections shouldBe 2
        discardDecision.maxSelections shouldBe 2

        val hand = driver.getHand(opponent)
        driver.submitDecision(opponent, CardsSelectedResponse(discardDecision.id, listOf(hand[0], hand[1])))

        // Now opponent should be offered to copy
        driver.isPaused shouldBe true
        val copyDecision = driver.pendingDecision
        copyDecision.shouldBeInstanceOf<YesNoDecision>()

        // Decline
        driver.submitDecision(opponent, YesNoResponse(copyDecision.id, false))

        // Opponent should have 2 fewer cards
        driver.getHandSize(opponent) shouldBe (opponentHandBefore - 2)
    }

    test("Chain of Smog - target player declines to copy") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val chain = driver.putCardInHand(activePlayer, "Chain of Smog")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Choose cards to discard
        val discardDecision = driver.pendingDecision
        discardDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val hand = driver.getHand(opponent)
        driver.submitDecision(opponent, CardsSelectedResponse(discardDecision.id, listOf(hand[0], hand[1])))

        // Offered to copy — decline
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(opponent, YesNoResponse(decision.id, false))

        // Chain ends, game continues normally
        driver.isPaused shouldBe false
    }

    test("Chain of Smog - target player copies and chooses new target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activeHandBefore = driver.getHandSize(activePlayer)
        val oppHandBefore = driver.getHandSize(opponent)

        val chain = driver.putCardInHand(activePlayer, "Chain of Smog")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Step 1: Opponent chooses cards to discard
        val discardDecision = driver.pendingDecision
        discardDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val oppHand = driver.getHand(opponent)
        driver.submitDecision(opponent, CardsSelectedResponse(discardDecision.id, listOf(oppHand[0], oppHand[1])))

        // Step 2: Opponent accepts copy
        val copyDecision = driver.pendingDecision
        copyDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(opponent, YesNoResponse(copyDecision.id, true))

        // Step 3: Opponent selects active player as the target for the copy
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(opponent, CardsSelectedResponse(targetDecision.id, listOf(activePlayer)))

        // Copy is on the stack - both pass to resolve
        driver.bothPass()

        // Step 4: Active player must choose 2 cards to discard
        driver.isPaused shouldBe true
        val apDiscardDecision = driver.pendingDecision
        apDiscardDecision.shouldBeInstanceOf<SelectCardsDecision>()
        apDiscardDecision.minSelections shouldBe 2
        val apHand = driver.getHand(activePlayer)
        driver.submitDecision(activePlayer, CardsSelectedResponse(apDiscardDecision.id, listOf(apHand[0], apHand[1])))

        // Step 5: Active player is offered to copy — decline
        val apCopyDecision = driver.pendingDecision
        apCopyDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(activePlayer, YesNoResponse(apCopyDecision.id, false))

        // Verify: active player lost 2 cards, opponent lost 2 cards
        driver.getHandSize(activePlayer) shouldBe (activeHandBefore - 2)
        driver.getHandSize(opponent) shouldBe (oppHandBefore - 2)
    }

    test("Chain of Smog - can target yourself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(activePlayer)

        val chain = driver.putCardInHand(activePlayer, "Chain of Smog")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(activePlayer))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Active player chooses cards to discard
        val discardDecision = driver.pendingDecision
        discardDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val hand = driver.getHand(activePlayer)
        driver.submitDecision(activePlayer, CardsSelectedResponse(discardDecision.id, listOf(hand[0], hand[1])))

        // Active player should be asked to copy (they targeted themselves)
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()

        // Decline
        driver.submitDecision(activePlayer, YesNoResponse(decision.id, false))

        // Should have discarded 2 cards
        driver.getHandSize(activePlayer) shouldBe (handBefore - 2)
    }
})
