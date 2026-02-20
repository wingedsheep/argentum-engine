package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Trade Secrets.
 *
 * Trade Secrets: {1}{U}{U}
 * Sorcery
 * Target opponent draws two cards, then you draw up to four cards.
 * That opponent may repeat this process as many times as they choose.
 */
class TradeSecretsTest : FunSpec({

    val TradeSecrets = CardDefinition.sorcery(
        name = "Trade Secrets",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        oracleText = "Target opponent draws two cards, then you draw up to four cards. That opponent may repeat this process as many times as they choose.",
        script = CardScript.spell(
            targets = arrayOf(TargetOpponent()),
            effect = Effects.RepeatWhile(
                body = Effects.Composite(
                    DrawCardsEffect(count = 2, target = EffectTarget.ContextTarget(0)),
                    DrawUpToEffect(maxCards = 4, target = EffectTarget.Controller)
                ),
                repeatCondition = RepeatCondition.PlayerChooses(
                    decider = EffectTarget.ContextTarget(0),
                    prompt = "Repeat the process? (You draw 2 cards, opponent draws up to 4)",
                    yesText = "Repeat",
                    noText = "Stop"
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TradeSecrets))
        return driver
    }

    test("Trade Secrets - opponent draws 2, controller draws up to 4, opponent declines repeat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activeHandBefore = driver.getHandSize(activePlayer)
        val opponentHandBefore = driver.getHandSize(opponent)

        // Cast Trade Secrets targeting opponent
        val tradeSecrets = driver.putCardInHand(activePlayer, "Trade Secrets")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        val castResult = driver.castSpell(activePlayer, tradeSecrets, targets = listOf(opponent))
        castResult.isSuccess shouldBe true

        // Resolve - both pass priority on the stack
        driver.bothPass()

        // After resolution: opponent drew 2, now controller must choose 0-4
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseNumberDecision>()
        decision.playerId shouldBe activePlayer

        // Controller chooses to draw 4
        driver.submitDecision(activePlayer, NumberChosenResponse(decision.id, 4))

        // Now opponent gets asked whether to repeat
        val repeatDecision = driver.pendingDecision
        repeatDecision.shouldBeInstanceOf<YesNoDecision>()
        repeatDecision.playerId shouldBe opponent

        // Opponent declines
        driver.submitDecision(opponent, YesNoResponse(repeatDecision.id, false))

        // Verify: opponent drew 2, controller drew 4
        driver.getHandSize(opponent) shouldBe opponentHandBefore + 2
        // Controller: started with activeHandBefore, +1 for put in hand, -1 for cast, +4 drawn
        driver.getHandSize(activePlayer) shouldBe activeHandBefore + 4
    }

    test("Trade Secrets - opponent repeats once then stops") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activeHandBefore = driver.getHandSize(activePlayer)
        val opponentHandBefore = driver.getHandSize(opponent)

        val tradeSecrets = driver.putCardInHand(activePlayer, "Trade Secrets")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        val castResult = driver.castSpell(activePlayer, tradeSecrets, targets = listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Round 1: Controller chooses to draw 3
        val decision1 = driver.pendingDecision
        decision1.shouldBeInstanceOf<ChooseNumberDecision>()
        driver.submitDecision(activePlayer, NumberChosenResponse(decision1.id, 3))

        // Opponent chooses to repeat
        val repeat1 = driver.pendingDecision
        repeat1.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(opponent, YesNoResponse(repeat1.id, true))

        // Round 2: Opponent drew 2 more, now controller chooses again
        val decision2 = driver.pendingDecision
        decision2.shouldBeInstanceOf<ChooseNumberDecision>()
        decision2.playerId shouldBe activePlayer
        driver.submitDecision(activePlayer, NumberChosenResponse(decision2.id, 4))

        // Opponent asked again
        val repeat2 = driver.pendingDecision
        repeat2.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(opponent, YesNoResponse(repeat2.id, false))

        // Total: opponent drew 2 + 2 = 4, controller drew 3 + 4 = 7
        driver.getHandSize(opponent) shouldBe opponentHandBefore + 4
        driver.getHandSize(activePlayer) shouldBe activeHandBefore + 7
    }

    test("Trade Secrets - controller chooses to draw 0 cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activeHandBefore = driver.getHandSize(activePlayer)
        val opponentHandBefore = driver.getHandSize(opponent)

        val tradeSecrets = driver.putCardInHand(activePlayer, "Trade Secrets")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        val castResult = driver.castSpell(activePlayer, tradeSecrets, targets = listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Controller chooses to draw 0
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseNumberDecision>()
        driver.submitDecision(activePlayer, NumberChosenResponse(decision.id, 0))

        // Opponent declines repeat
        val repeatDecision = driver.pendingDecision
        repeatDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(opponent, YesNoResponse(repeatDecision.id, false))

        // Opponent drew 2, controller drew 0
        driver.getHandSize(opponent) shouldBe opponentHandBefore + 2
        driver.getHandSize(activePlayer) shouldBe activeHandBefore + 0
    }
})
