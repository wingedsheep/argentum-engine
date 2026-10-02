package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The counting band — [Amounts.count] as one vocabulary, and the three positions that slot it: the
 * characteristic-defining stat box, the counted verbs' "equal to …" clause, and the where-clause
 * sentences that already existed.
 *
 * The assertions worth having here are the ones about **which** value a phrase denotes and which it
 * refuses. A tally of your graveyard and a tally of your battlefield round-trip equally well under
 * each other's reading — that is precisely the reversible-but-wrong class, and it is the bug the
 * differential found in a hand-written Revenant on the day this band could read its line.
 */
class AmountsTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    fun dynamic(line: String): DynamicAmount =
        (fragment(line).dynamicPower as CharacteristicValue.Dynamic).source

    /** The power bonus of a line whose only ability is a `~ gets …` static. */
    fun dynamicStat(line: String): DynamicAmount =
        (fragment(line).script.staticAbilities.single() as GrantDynamicStats).powerBonus

    // ---------------------------------------------------------------------------------------
    // The vocabulary
    // ---------------------------------------------------------------------------------------

    "a battlefield tally names whose battlefield it scans" {
        dynamic("~'s power and toughness are each equal to the number of Swamps you control.") shouldBe
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land.withSubtype("Swamp"))
        dynamic("~'s power and toughness are each equal to the number of creatures on the battlefield.") shouldBe
            DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature)
        roundTrips("~'s power and toughness are each equal to the number of Swamps you control.")
        roundTrips("~'s power and toughness are each equal to the number of creatures on the battlefield.")
    }

    // Revenant's bug, as a property. The two readings are byte-identical apart from the zone.
    "a zone tally counts cards, and is not the battlefield tally" {
        dynamic("~'s power and toughness are each equal to the number of creature cards in your graveyard.") shouldBe
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
        dynamic("~'s power and toughness are each equal to the number of creature cards in all graveyards.") shouldBe
            DynamicAmount.Count(Player.Each, Zone.GRAVEYARD, GameObjectFilter.Creature)
        roundTrips("~'s power and toughness are each equal to the number of creature cards in your graveyard.")
        roundTrips("~'s power and toughness are each equal to the number of creature cards in all graveyards.")
    }

    "an unfiltered zone tally is its own sentence, with no noun to slot" {
        dynamic("~'s power and toughness are each equal to the number of cards in your hand.") shouldBe
            DynamicAmount.Count(Player.You, Zone.HAND)
        roundTrips("~'s power and toughness are each equal to the number of cards in your hand.")
        roundTrips("~'s power and toughness are each equal to the number of cards in your graveyard.")
    }

    "the aggregation layer owns one field and leaves the noun phrase alone" {
        dynamic("~'s power is equal to the greatest mana value among artifacts you control.") shouldBe
            DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Artifact,
                Aggregation.MAX,
                CardNumericProperty.MANA_VALUE,
            )
        dynamic("~'s power is equal to the number of colors among permanents you control.") shouldBe
            DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Permanent,
                Aggregation.DISTINCT_COLORS,
            )
        roundTrips("~'s power is equal to the greatest mana value among artifacts you control.")
        roundTrips("~'s power is equal to the number of colors among permanents you control.")
        roundTrips("~'s power is equal to the greatest power among creatures you control.")
        roundTrips("~'s power is equal to the number of basic land types among lands you control.")
    }

    "the multiplier is a layer, and only the word English has" {
        dynamic("~'s power and toughness are each equal to twice the number of cards in your hand.") shouldBe
            DynamicAmount.Multiply(DynamicAmount.Count(Player.You, Zone.HAND), 2)
        roundTrips("~'s power and toughness are each equal to twice the number of cards in your hand.")
        declines("~'s power and toughness are each equal to three times the number of cards in your hand.")
    }

    "your life total is a count with no noun in it at all" {
        dynamic("~'s power and toughness are each equal to your life total.") shouldBe
            DynamicAmount.YourLifeTotal
        roundTrips("~'s power and toughness are each equal to your life total.")
    }

    // ---------------------------------------------------------------------------------------
    // The characteristic-defining line — three shapes and the pairing rule
    // ---------------------------------------------------------------------------------------

    "the joined form defines both characteristics from one clause" {
        val fragment = fragment("~'s power and toughness are each equal to the number of lands you control.")
        val count = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land)
        fragment.dynamicPower shouldBe CharacteristicValue.Dynamic(count)
        fragment.dynamicToughness shouldBe CharacteristicValue.Dynamic(count)
        fragment.script shouldBe com.wingedsheep.sdk.model.CardScript.EMPTY
    }

    "each single-characteristic form defines only its own half" {
        fragment("~'s power is equal to the number of lands you control.").dynamicToughness shouldBe null
        fragment("~'s toughness is equal to the number of lands you control.").dynamicPower shouldBe null
        roundTrips("~'s power is equal to the number of lands you control.")
        roundTrips("~'s toughness is equal to the number of lands you control.")
    }

    // Yavimaya Kavu: two lines, two characteristics, one card.
    "two single-characteristic lines fold, and two of the same one do not" {
        val power = fragment("~'s power is equal to the number of lands you control.")
        val toughness = fragment("~'s toughness is equal to the number of creatures you control.")
        val folded = power.merge(toughness)!!
        folded.dynamicPower shouldBe
            CharacteristicValue.Dynamic(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land))
        folded.dynamicToughness shouldBe
            CharacteristicValue.Dynamic(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature))
        power.merge(power) shouldBe null
    }

    // Lhurgoyf's shape: one amount, named once and used twice, with the offset on the second half.
    "the plus-one sibling stores the amount once and adds the offset" {
        val line = "~'s power is equal to the number of creature cards in all graveyards " +
            "and its toughness is equal to that number plus 1."
        val count = DynamicAmount.Count(Player.Each, Zone.GRAVEYARD, GameObjectFilter.Creature)
        fragment(line).dynamicPower shouldBe CharacteristicValue.Dynamic(count)
        fragment(line).dynamicToughness shouldBe CharacteristicValue.DynamicWithOffset(count, 1)
        roundTrips(line)
    }

    // ---------------------------------------------------------------------------------------
    // The counted verbs' second spelling
    // ---------------------------------------------------------------------------------------

    // Life joined the band with the life-amount rewrite: the seven life rows became one
    // `LifeChange` table over `countedStepPair`, so the clause the damage verbs read is now a
    // second *input* spelling rather than a decline. It is an `alsoSpelled` alternate, not a
    // second printed form — "for each" outnumbers "equal to" 131 to 23 in print and stays
    // canonical, so the clause reads and reprints as the numeral form.
    "life reads its numeral, and the damage verbs' clause reprints as it" {
        roundTrips("You gain 3 life.")
        Grammar.abilityLine.printLine(
            fragment("You gain life equal to the number of creatures you control.")
        ) shouldBe "You gain 1 life for each creature you control."
    }

    "damage puts the clause where the amount's shape says it goes" {
        // A tally is a heavy noun phrase and trails the recipient…
        roundTrips("~ deals damage to target creature equal to the number of Mountains you control.")
        roundTrips("~ deals damage to any target equal to the number of cards in your hand.")
        // …and a property of an object is light and leads.
        roundTrips("~ deals damage equal to the number of +1/+1 counters on ~ to any target.")
        roundTrips("~ deals damage equal to the number of +1/+1 counters on ~ to target creature.")
    }

    // ---------------------------------------------------------------------------------------
    // Where a tally counts — [Amounts.scopes], the layer five families each froze a row of
    // ---------------------------------------------------------------------------------------

    "the where-clause is three rows of one layer, and the families share them" {
        // "the number of …" had both printed rows already; the sentences that count had one each.
        roundTrips("~ gets +1/+1 for each creature on the battlefield.")
        roundTrips("~ gets +1/+0 for each artifact you control.")
        roundTrips("You gain 1 life for each creature on the battlefield.")
        roundTrips("You gain 2 life for each creature you control.")
        roundTrips("Draw a card for each creature you control.")
        roundTrips("This spell costs {1} less to cast for each creature you control.")
        roundTrips("This spell costs {1} less to cast for each creature on the battlefield.")
    }

    "the empty row parses and prints as the clause it leaves out" {
        // English omits "on the battlefield" and means it, so the bare spelling is an alternate of
        // the printed one rather than a value of its own.
        Grammar.abilityLine.printLine(fragment("You gain 1 life for each attacking creature.")) shouldBe
            "You gain 1 life for each attacking creature on the battlefield."
        Grammar.abilityLine.printLine(fragment("Draw a card for each attacking creature.")) shouldBe
            "Draw a card for each attacking creature on the battlefield."
        Grammar.abilityLine.printLine(
            fragment("This spell costs {1} less to cast for each attacking creature.")
        ) shouldBe "This spell costs {1} less to cast for each attacking creature on the battlefield."
    }

    "a counted noun phrase says where it counts exactly once" {
        // The clause and the noun phrase's own controller layer are the same layer, so a row with a
        // surface refuses a filter that already carries one — otherwise "for each creature you
        // control" has two readings with two models, which is the ambiguity this grammar never
        // resolves by ordering an alternation.
        declines("You gain 1 life for each creature you control on the battlefield.")
        declines("~ gets +1/+1 for each creature you control on the battlefield.")
        // The empty row refuses the clauses the " you control" and " your opponents control" rows
        // print, so a controller the layer has no row for still reaches the model through the noun
        // phrase. (It reads rather than round-trips: the noun phrase has a canonical spelling of its
        // own.)
        fragment("You gain 1 life for each creature you don't control.")
        // The opponents' tally is the row, `EachOpponent` — the hand-written spelling — and the
        // noun phrase's opponent clause over the whole battlefield is not a second reading of it.
        roundTrips("~ gets +1/+1 for each Swamp your opponents control.")
        dynamicStat("~ gets +1/+1 for each Swamp your opponents control.")
            .shouldBeInstanceOf<DynamicAmount.AggregateBattlefield>().player shouldBe Player.EachOpponent
        declines("You gain 1 life for each creature an opponent controls.")
    }

    "the modifier pair is two numbers, not one" {
        // "+1/+0" is the bare tally beside a Fixed(0) — Nim Lasher's model, and every card in the
        // family. The rule used to require the two halves to agree, which is why it read none of them.
        dynamicStat("~ gets +1/+0 for each artifact you control.") shouldBe
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Artifact)
        roundTrips("~ gets +0/+1 for each Mountain you control.")
        roundTrips("~ gets +2/+2 for each face-down creature on the battlefield.")
    }

    // The minority order for each domain parses and comes back as the majority one: the reading
    // survived, only the spelling moved, which is what an `alternate` is for.
    "the minority word order reads and reprints as the canonical one" {
        Grammar.abilityLine.printLine(
            fragment("~ deals damage equal to the number of Mountains you control to target creature.")
        ) shouldBe "~ deals damage to target creature equal to the number of Mountains you control."
        Grammar.abilityLine.printLine(
            fragment("~ deals damage to any target equal to the number of +1/+1 counters on ~.")
        ) shouldBe "~ deals damage equal to the number of +1/+1 counters on ~ to any target."
    }

    // The aggregation boundary is the *player*, which no `AggregateBattlefield` scope can be:
    // "on the battlefield" flattens the table into one set and counts it, giving 5 where this
    // gives 3. Both spellings parse, so the assertion is about which value each denotes.
    "the superlative counts each player separately and takes the largest" {
        fragment("~ enters with a number of suspect counters on it equal to the greatest number of creatures a player controls.")
            .script.replacementEffects.single()
            .let { it as com.wingedsheep.sdk.scripting.EntersWithDynamicCounters }
            .count shouldBe DynamicAmount.GreatestAmongPlayers(
                players = Player.Each,
                inner = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
            )
        roundTrips("~ enters with X suspect counters on it, where X is the greatest number of creatures a player controls.")
        roundTrips("~ enters with X charge counters on it, where X is the greatest number of artifacts an opponent controls.")
    }

    "the collective count and the superlative are different values of the same words" {
        fragment("~ gets +1/+1 for each creature on the battlefield.") shouldNotBe
            fragment("~ enters with X +1/+1 counters on it, where X is the greatest number of creatures a player controls.")
    }
})
