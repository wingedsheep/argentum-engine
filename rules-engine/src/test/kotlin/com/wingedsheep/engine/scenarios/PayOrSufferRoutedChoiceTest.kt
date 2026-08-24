package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `PayOrSufferEffect.consequenceDescription` on the **choice-of-costs** path.
 *
 * A single-cost pay-or-suffer asks one question, and the override reaches it directly. A
 * `Costs.pay.Choice` asks *two*: first "choose one" over the cost options plus the consequence, and
 * then — once a cost is picked — the cost-specific prompt, rebuilt from a fresh single-cost
 * `PayOrSufferEffect` on the far side of a continuation. Both used to drop the override:
 *
 *  - the option list read `effect.suffer.description` directly rather than going through
 *    `describeConsequence`, and
 *  - the continuation carried no `consequenceDescription`, so the rebuilt effect always fell back
 *    to the generated text.
 *
 * That is invisible while the payer is the ability's controller, because the generated description
 * is an imperative fragment addressed to exactly that player. It stops being invisible the moment
 * `player` routes the question at an opponent — which is the only reason the field exists. So this
 * test uses the shape no shipped card has yet: routed payer *and* a choice of costs.
 */
class PayOrSufferRoutedChoiceTest : FunSpec({

    /** The consequence in the payer's own terms — what both prompts must say. */
    val victimsWords = "Toll Collector's controller draws two cards"

    /**
     * "When this creature enters, unless an opponent pays {1} or 1 life, you draw two cards."
     *
     * `GainControl`-style asymmetry reduced to its bones: the suffer effect's own description
     * ("draw 2 cards") is written from the *controller's* side, so offering it verbatim to the
     * opponent invites them to draw the cards they are actually being taxed to prevent.
     */
    val TollCollector = CardDefinition(
        name = "Toll Collector",
        manaCost = ManaCost.parse("{2}"),
        typeLine = TypeLine.artifactCreature(setOf(Subtype("Construct"))),
        oracleText = "When this creature enters, unless an opponent pays {1} or 1 life, " +
            "you draw two cards.",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = EventPattern.ZoneChangeEvent(to = Zone.BATTLEFIELD),
                binding = TriggerBinding.SELF,
                effect = PayOrSufferEffect(
                    cost = Costs.pay.Choice(listOf(Costs.pay.Mana("{1}"), Costs.pay.PayLife(1))),
                    suffer = Effects.DrawCards(2),
                    player = EffectTarget.PlayerRef(Player.AnOpponent),
                    consequenceDescription = victimsWords,
                ),
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TollCollector)
        return driver
    }

    /**
     * Put the Collector onto the battlefield and resolve its enters trigger, leaving the routed
     * choice pending. The opponent is given both a floating mana and a life total high enough that
     * each cost option is genuinely affordable.
     */
    fun openTheChoice(driver: GameTestDriver): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), skipMulligans = true)
        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(com.wingedsheep.sdk.core.Step.PRECOMBAT_MAIN)

        driver.giveMana(victim, Color.GREEN, 1)
        val card = driver.putCardInHand(me, "Toll Collector")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, card).isSuccess shouldBe true
        driver.bothPass() // resolve the permanent
        driver.bothPass() // resolve the enters trigger
        return me to victim
    }

    test("the choice list offers the consequence in the payer's words, not the controller's") {
        val driver = createDriver()
        val (_, victim) = openTheChoice(driver)

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("the question goes to the opponent the effect routed it at") {
            decision.playerId shouldBe victim
        }
        withClue("the last option is the consequence, phrased for the player being asked") {
            decision.options.last() shouldBe victimsWords.replaceFirstChar { it.uppercase() }
        }
        withClue("and never the suffer effect's controller-side wording") {
            decision.options.none { it.contains("draw 2 cards", ignoreCase = true) } shouldBe true
        }
    }

    test("the follow-up prompt for the chosen cost asks in the same words") {
        val driver = createDriver()
        val (_, victim) = openTheChoice(driver)

        val choice = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        // Take the mana option — the executor rebuilds a single-cost effect on the far side of the
        // continuation, and that rebuild is where the override used to be lost.
        driver.submitDecision(victim, OptionChosenResponse(choice.id, 0))

        val followUp = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        withClue("still the opponent's question") { followUp.playerId shouldBe victim }
        withClue("the consequence clause survived the continuation round-trip") {
            // Asserted as a suffix rather than the whole string: the cost half is the mana
            // renderer's business, the consequence half is this test's.
            followUp.prompt.endsWith(" or $victimsWords?") shouldBe true
        }
        withClue("and did not revert to the generated controller-side wording") {
            followUp.prompt.contains("draw 2 cards", ignoreCase = true) shouldBe false
        }
    }
})
