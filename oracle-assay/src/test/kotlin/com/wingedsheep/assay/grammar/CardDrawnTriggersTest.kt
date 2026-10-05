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
 * The every-draw trigger — "Whenever you draw a card, …" — `DrawEvent`, the sibling of the
 * ordinal trigger in [NthCardDrawnTriggersTest].
 */
class CardDrawnTriggersTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun ability(line: String): TriggeredAbility =
        fragment(line).script.triggeredAbilities.single()

    // Clinquant Skymage's golden is this model exactly.
    "a draw trigger is the ability a card author writes from the same sentence" {
        ability("Whenever you draw a card, put a +1/+1 counter on ~.") shouldBe
            TriggeredAbility(
                id = AbilityId("trigger"),
                trigger = EventPattern.DrawEvent(player = Player.You),
                binding = SdkTriggers.you.draws().binding,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            )
    }

    "the drawer is a field on the event, so the three subjects are three rows" {
        (ability("Whenever you draw a card, draw a card.").trigger as EventPattern.DrawEvent).player shouldBe
            Player.You
        (ability("Whenever an opponent draws a card, draw a card.").trigger as EventPattern.DrawEvent).player shouldBe
            Player.EachOpponent
        (ability("Whenever a player draws a card, draw a card.").trigger as EventPattern.DrawEvent).player shouldBe
            Player.Each
    }

    // Orcish Bowmasters: CR 504.1's turn-based draw is excluded by a flag on the event.
    "the draw-step exception is the event's flag" {
        val trigger = ability(
            "Whenever an opponent draws a card except the first one they draw in each of their draw steps, " +
                "~ deals 1 damage to any target.",
        ).trigger
        trigger shouldBe EventPattern.DrawEvent(player = Player.EachOpponent, exceptFirstInDrawStep = true)
    }

    // Scrawling Crawler: "that player" is the drawer.
    "that player is the triggering player" {
        ability("Whenever an opponent draws a card, that player loses 1 life.").effect shouldBe
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    // Scroll of Griselbrand: beside a declared target, "that player" names the target, not the drawer.
    "that player after a declared target declines, even across a scoped clause" {
        Grammar.abilityLine.parseLine(
            "{1}, Sacrifice ~: Target opponent discards a card. If you control a Demon, that player loses 3 life.",
        ).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "the subject and its verb agree, so a crossed pair declines" {
        Grammar.abilityLine.parseLine("Whenever you draws a card, draw a card.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.parseLine("Whenever an opponent draw a card, draw a card.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "every draw rule can print what it parses" {
        listOf(
            "Whenever you draw a card, put a +1/+1 counter on ~.",
            "Whenever you draw a card, you may put a hoofprint counter on ~.",
            "Whenever you draw a card, you gain 2 life.",
            "Whenever an opponent draws a card, you may draw two cards.",
            "Whenever an opponent draws a card, that player loses 1 life.",
            "Whenever a player draws a card, ~ deals 1 damage to that player.",
            "Whenever an opponent draws a card except the first one they draw in each of their draw steps, " +
                "~ deals 1 damage to any target.",
        ).forEach { roundTrips(it) }
    }
})
