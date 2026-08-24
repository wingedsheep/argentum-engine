package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers as SdkTriggers
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Bloomburrow life-and-expend band: the set's own trigger events, the life-state conditions its
 * Bats check, and the two "each opponent" clauses that pay them off.
 *
 * The band is rows in existing families rather than machinery of its own, which is what a set is
 * supposed to cost — so the tests are about the two things a row can still get wrong. One is the
 * *product*: an expend threshold is a slot, so every threshold has to read and print, and the payoff
 * clause has to be the whole step vocabulary rather than the handful of sentences Bloomburrow prints.
 * The other is *disjointness*: "each opponent loses 2 life" and "target player loses 2 life" are one
 * verb over two recipients, and only one of them declares a target requirement.
 */
class LifeAndExpendTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    /**
     * A line whose reading is right and whose spelling is normalized — the VARIANT verdict. Asserted
     * as "reads, and prints back as [canonical]" so the test says which of two printed forms the
     * grammar owns rather than merely that one of them survives.
     */
    fun variantOf(line: String, canonical: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe canonical
        Grammar.abilityLine.printLine(fragment(canonical)) shouldBe canonical
    }

    fun ability(line: String): TriggeredAbility =
        fragment(line).script.triggeredAbilities.single()

    // Bark-Knuckle Boxer's golden is this model exactly. The threshold is read off the event rather
    // than compared against a constant, which is what makes it a slot.
    "an expend trigger is the ability the card author writes from the same sentence" {
        ability("Whenever you expend 4, ~ gains indestructible until end of turn.").trigger shouldBe
            EventPattern.ExpendEvent(threshold = 4)
        roundTrips("Whenever you expend 4, ~ gains indestructible until end of turn.")
    }

    // The threshold is a numeral, not a number word: Oracle writes "expend 4" where it writes
    // "draw two cards". Muerra, Trash Tactician is the only card printing a second value, and
    // nothing in the sentence says 4 and 8 are the only two.
    "every expend threshold reads and prints" {
        listOf(1, 2, 4, 8, 12).forEach { n ->
            ability("Whenever you expend $n, you gain 3 life.").trigger shouldBe
                EventPattern.ExpendEvent(threshold = n)
            roundTrips("Whenever you expend $n, you gain 3 life.")
        }
        declines("Whenever you expend four, you gain 3 life.")
    }

    // The prefix slots `Steps.step`, so an expend trigger inherits every effect rule rather than the
    // five sentences Bloomburrow happens to print behind it.
    "the payoff clause is the whole step vocabulary" {
        listOf(
            "Whenever you expend 4, ~ gets +2/+1 until end of turn.",
            "Whenever you expend 4, put a +1/+1 counter on ~.",
            "Whenever you expend 4, ~ deals 2 damage to each opponent.",
            "Whenever you expend 4, you gain 3 life.",
            "Whenever you expend 4, draw a card.",
            "Whenever you expend 8, destroy target creature.",
        ).forEach { roundTrips(it) }
    }

    "the life-change triggers and the gift payoff are the specs the SDK publishes" {
        ability("Whenever you gain life, each opponent loses 1 life.").trigger shouldBe
            SdkTriggers.YouGainLife.event
        ability("Whenever you lose life, draw a card.").trigger shouldBe SdkTriggers.YouLoseLife.event
        ability("Whenever you give a gift, draw a card.").trigger shouldBe SdkTriggers.YouGiveAGift.event
        listOf(
            "Whenever you gain life, each opponent loses 1 life.",
            "Whenever you lose life, draw a card.",
            "Whenever you give a gift, draw a card.",
        ).forEach { roundTrips(it) }
    }

    // Wax-Wane Witness. "During your turn" is a `triggerRestriction`, which the trigger rule
    // deliberately never writes — CR 603.2's restriction on the event is not CR 603.4's
    // intervening-if, and the engine re-checks only the second. So the sentence declines rather than
    // printing back as a card that means something else.
    "the during-your-turn narrowing declines rather than becoming a condition" {
        declines("Whenever you gain or lose life during your turn, ~ gets +1/+0 until end of turn.")
    }

    // Starlit Soothsayer, Lunar Convocation, Essence Channeler, Stromkirk Bloodthief. Each clause is
    // one named condition, and each has exactly one printed spelling in the corpus — which is why
    // `YouLostLifeThisTurn`'s present perfect is canonical rather than an alternate.
    "the life-state conditions are one clause each" {
        ability("At the beginning of your end step, if you gained or lost life this turn, surveil 1.")
            .interveningIf shouldBe Conditions.YouGainedOrLostLifeThisTurn
        ability("At the beginning of your end step, if you gained and lost life this turn, draw a card.")
            .interveningIf shouldBe Conditions.YouGainedAndLostLifeThisTurn
        ability("At the beginning of your end step, if you gained life this turn, each opponent loses 1 life.")
            .interveningIf shouldBe Conditions.YouGainedLifeThisTurn
        ability("At the beginning of your end step, if an opponent lost life this turn, draw a card.")
            .interveningIf shouldBe Conditions.OpponentLostLifeThisTurn
        listOf(
            "At the beginning of your end step, if you gained or lost life this turn, surveil 1.",
            "At the beginning of your end step, if you gained and lost life this turn, draw a card.",
            "At the beginning of your end step, if you gained life this turn, each opponent loses 1 life.",
            "At the beginning of your end step, if an opponent lost life this turn, draw a card.",
        ).forEach { roundTrips(it) }
        // Essence Channeler prints the condition *fronted*. That is [Statics]' existing alternate
        // spelling of a conditional self-static — the model has no room for which end the clause
        // sits at — so the line reads and comes back in the trailing form the grammar owns.
        variantOf(
            "As long as you've lost life this turn, ~ has flying and vigilance.",
            "~ has flying and vigilance as long as you've lost life this turn.",
        )
    }

    // Glidedive Duo, whole: the drain is a clause *sequence*, not a `DrainLife`. Exsanguinate's
    // "you gain life equal to the life lost this way" is the type that folds the two together, and
    // it is a different printed sentence.
    "each opponent loses life is a recipient the model names, not one it targets" {
        fragment("When ~ enters, each opponent loses 2 life and you gain 2 life.")
            .script.triggeredAbilities.single().effect shouldBe Effects.Composite(
            listOf(
                Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)),
                Effects.GainLife(2, EffectTarget.Controller),
            )
        )
        // Glidedive Duo prints the clauses joined by "and", which [Steps.tailsOf] reads as an
        // alternate and prints back as the canonical sequence: a `CompositeEffect` has no room for
        // the conjunction, so one of the two spellings has to be the one that prints.
        variantOf(
            "When ~ enters, each opponent loses 2 life and you gain 2 life.",
            "When ~ enters, each opponent loses 2 life. You gain 2 life.",
        )
        // The targeted sibling still declares its requirement; the two never stand in one slot.
        fragment("Target player loses 2 life.").script.targetRequirements.size shouldBe 1
        fragment("Each opponent loses 2 life.").script.targetRequirements.size shouldBe 0
    }

    // Teapot Slinger and Coruscation Mage. The "equal to …" sibling comes from the same call site,
    // so it costs nothing beyond the amount vocabulary already being there.
    "damage to each opponent is a row beside the targeted recipients" {
        listOf(
            "~ deals 2 damage to each opponent.",
            "~ deals 1 damage to each opponent.",
        ).forEach { roundTrips(it) }
        fragment("~ deals 2 damage to each opponent.").script.targetRequirements.size shouldBe 0
        fragment("~ deals 2 damage to target opponent.").script.targetRequirements.size shouldBe 1
    }
})
