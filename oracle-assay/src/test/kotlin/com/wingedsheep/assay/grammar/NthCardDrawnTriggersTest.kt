package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers as SdkTriggers
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The ordinal draw trigger — "Whenever you draw your second card each turn, …" — the
 * `NthCardDrawnEvent` twin of the ordinal cast trigger in [SpellCastTriggersTest].
 */
class NthCardDrawnTriggersTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun ability(line: String): TriggeredAbility =
        fragment(line).script.triggeredAbilities.single()

    // Knights of Dol Amroth's golden is this model exactly.
    "an ordinal draw trigger is the ability a card author writes from the same sentence" {
        ability("Whenever you draw your second card each turn, put a +1/+1 counter on ~.") shouldBe
            TriggeredAbility(
                id = AbilityId("trigger"),
                trigger = EventPattern.NthCardDrawnEvent(nthCard = 2, player = Player.You),
                binding = SdkTriggers.you.drawsNth(2).binding,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            )
    }

    "the drawer is a field on the event, so the three subjects are three rows" {
        (ability("Whenever you draw your second card each turn, draw a card.").trigger
            as EventPattern.NthCardDrawnEvent).player shouldBe Player.You
        (ability("Whenever an opponent draws their second card each turn, draw a card.").trigger
            as EventPattern.NthCardDrawnEvent).player shouldBe Player.EachOpponent
        (ability("Whenever a player draws their second card each turn, draw a card.").trigger
            as EventPattern.NthCardDrawnEvent).player shouldBe Player.Each
    }

    "the possessive tracks the subject, so a crossed pair declines" {
        Grammar.abilityLine.parseLine("Whenever you draw their second card each turn, draw a card.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.parseLine("Whenever an opponent draws your second card each turn, draw a card.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // One ability over two events (Lady Octopus) is not a value the single ordinal slot can hold.
    "two ordinals joined by or decline" {
        Grammar.abilityLine.parseLine("Whenever you draw your first or second card each turn, draw a card.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "every ordinal draw rule can print what it parses" {
        listOf(
            "Whenever you draw your first card each turn, draw a card.",
            "Whenever you draw your second card each turn, put a +1/+1 counter on ~.",
            "Whenever you draw your third card each turn, draw a card.",
            "Whenever you draw your second card each turn, untap ~.",
            "Whenever an opponent draws their second card each turn, draw a card.",
            "Whenever a player draws their second card each turn, draw a card.",
        ).forEach { roundTrips(it) }
    }
})
