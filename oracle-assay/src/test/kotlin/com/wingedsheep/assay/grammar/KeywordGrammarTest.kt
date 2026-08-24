package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Reminders
import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class KeywordGrammarTest : StringSpec({

    fun parse(line: String): List<KeywordAbility> =
        Grammar.abilityLine.parseLine(line)
            .shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value.keywordAbilities

    fun script(line: String): CardScript =
        Grammar.abilityLine.parseLine(line)
            .shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value.script

    fun roundTrips(line: String) {
        val fragment = Grammar.abilityLine.parseLine(line)
            .shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value
        Grammar.abilityLine.printLine(fragment) shouldBe line
    }

    "a simple keyword line round-trips" {
        parse("Flying") shouldContainExactly listOf(KeywordAbility.of(Keyword.FLYING))
        roundTrips("Flying")
    }

    "sentence case follows position, not the rule" {
        parse("Flying, vigilance") shouldContainExactly
            listOf(KeywordAbility.of(Keyword.FLYING), KeywordAbility.of(Keyword.VIGILANCE))
        roundTrips("Flying, vigilance")
        roundTrips("Vigilance, flying")
    }

    // Devoid (CR 702.114) prints as its own leading line on every Eldrazi that has it, and the
    // grammar reads it through the same `simple(...)` row as flying — which is the point: the row
    // feeds both the keyword-line list and the bare-keyword list, so the word can never be readable
    // as a line and unreadable when a sentence names it.
    "devoid reads and prints as a plain keyword line" {
        parse("Devoid") shouldContainExactly listOf(KeywordAbility.of(Keyword.DEVOID))
        roundTrips("Devoid")
        // Sentence case is applied at the line boundary, so the one rule serves both positions.
        roundTrips("Devoid, flying")
        roundTrips("Flying, devoid")
    }

    "devoid regenerates its printed reminder text" {
        Reminders.gloss(KeywordAbility.of(Keyword.DEVOID)) shouldBe "This card has no color."
        // The gloss is a property of the ability alone — devoid says "this card" whatever the card is.
        Reminders.gloss(KeywordAbility.of(Keyword.DEVOID), self = "this spell") shouldBe
            "This card has no color."
    }

    "a vanilla card's absent line is a rule, not a special case" {
        parse("") shouldContainExactly emptyList()
        roundTrips("")
    }

    "parameterized keywords carry their parameter both ways" {
        parse("Ward {2}") shouldContainExactly listOf(KeywordAbility.ward("{2}"))
        roundTrips("Ward {2}")
        roundTrips("Ward—Pay 3 life.")
        roundTrips("Annihilator 2")
        roundTrips("Crew 3")
        roundTrips("Flashback {3}{R}")
        roundTrips("Cycling {2}")
        roundTrips("Suspend 4—{1}{R}")
        roundTrips("Impending 4—{2}{W}{W}")
        roundTrips("Splice onto Arcane {1}{U}")
        roundTrips("Kicker {2}{G}")
        roundTrips("Multikicker {1}")
        roundTrips("Morph {2}{U}")
        roundTrips("Basic landcycling {1}{U}")
        roundTrips("Forestcycling {2}")
    }

    "protection reads every quality it can express" {
        parse("Protection from black") shouldContainExactly
            listOf(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))
        roundTrips("Protection from black")
        roundTrips("Protection from everything")
        roundTrips("Protection from each opponent")
        roundTrips("Protection from artifacts")
        roundTrips("Protection from Goblins")
    }

    // CR 702.16g: "protection from [A] and from [B]" is shorthand for two protection abilities.
    // The old reading — one `Protection(Colors([W, U]))` — round-tripped byte-exact while
    // disagreeing with every card that spells it, which is the class only the differential catches.
    "a multi-quality protection is two abilities, per CR 702.16g" {
        parse("Protection from white and from blue") shouldContainExactly listOf(
            KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)),
            KeywordAbility.Protection(ProtectionScope.Color(Color.BLUE)),
        )
        roundTrips("Protection from white and from blue")
    }

    "the join is over qualities, not colours, and covers hexproof too (CR 702.11f)" {
        parse("Protection from Demons and from Dragons") shouldContainExactly listOf(
            KeywordAbility.Protection(ProtectionScope.Subtype("Demon")),
            KeywordAbility.Protection(ProtectionScope.Subtype("Dragon")),
        )
        roundTrips("Protection from Demons and from Dragons")
        roundTrips("Hexproof from white and from black")
    }

    "three or more qualities take the printed Oxford comma" {
        parse("Protection from Vampires, from Werewolves, and from Zombies") shouldContainExactly listOf(
            KeywordAbility.Protection(ProtectionScope.Subtype("Vampire")),
            KeywordAbility.Protection(ProtectionScope.Subtype("Werewolf")),
            KeywordAbility.Protection(ProtectionScope.Subtype("Zombie")),
        )
        roundTrips("Protection from Vampires, from Werewolves, and from Zombies")
    }

    // A run is joined maximally on the way out, so the model decides the grouping and not the order
    // the rules happen to be tried in. Neighbours that are not part of the run keep their commas.
    "a run joins only its own neighbours" {
        roundTrips("Flying, protection from black and from red, trample")
    }

    "an irregular plural is not de-pluralized into a subtype that does not exist" {
        parse("Protection from Elves") shouldContainExactly
            listOf(KeywordAbility.Protection(ProtectionScope.Subtype("Elf")))
        roundTrips("Protection from Elves")
    }

    // The differential gate found this on its first run: "Plains" naively de-pluralizes to `Plain`,
    // which round-trips perfectly ("Plain" + "s") while naming a type the SDK does not have —
    // `Subtype.PLAINS` is `Plains`. Only checking against the SDK's own type list catches it.
    "an invariant plural keeps its own spelling rather than losing its s" {
        parse("Affinity for Plains") shouldContainExactly
            listOf(KeywordAbility.AffinityForSubtype(Subtype("Plains")))
        roundTrips("Affinity for Plains")
    }

    "the ordinary plural still wins where both readings would name a real type" {
        parse("Protection from Zombies") shouldContainExactly
            listOf(KeywordAbility.Protection(ProtectionScope.Subtype("Zombie")))
        roundTrips("Protection from Zombies")
    }

    "a consonant-y type pluralizes as -ies, and a vowel-y one does not" {
        roundTrips("Protection from Allies")
        roundTrips("Protection from Monkeys")
    }

    "a -ves plural resolves to its -f singular in both directions" {
        parse("Protection from Werewolves") shouldContainExactly
            listOf(KeywordAbility.Protection(ProtectionScope.Subtype("Werewolf")))
        roundTrips("Protection from Werewolves")
    }

    // The SDK publishes a list for creature and basic land types only, so artifact / enchantment /
    // nonbasic-land types are real subtypes it cannot confirm. Ranking rather than gating on the
    // list is what keeps them working: no candidate is known, so the ordinary reading stands.
    "a subtype the SDK's lists do not cover still reads by the ordinary rule" {
        parse("Affinity for Gates") shouldContainExactly
            listOf(KeywordAbility.AffinityForSubtype(Subtype("Gate")))
        roundTrips("Affinity for Gates")
        roundTrips("Affinity for Foods")
    }

    "a long real keyword line round-trips whole" {
        roundTrips("Flying, first strike, vigilance, trample, haste, protection from black and from red")
    }

    "the semicolon separator parses and normalizes to the canonical comma" {
        parse("Flying; banding") shouldContainExactly
            listOf(KeywordAbility.of(Keyword.FLYING), KeywordAbility.of(Keyword.BANDING))
        val fragment = Grammar.abilityLine.parseLine("Flying; banding")
            .shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value
        Grammar.abilityLine.printLine(fragment) shouldBe "Flying, banding"
    }

    // Both of this test's earlier examples have since been read, and the pair is the module's own
    // record of how a decline gets closed. "Enchant creature" turned out to be a plain
    // `TargetRequirement` and went to `Targets.enchant`; "Equip {2}" was the harder half of the same
    // Phase 1 finding — a keyword the SDK *lowers* at authoring time rather than storing — and is
    // now `Grammar.equipLine`, which reproduces the lowering through the SDK's own factory instead
    // of parsing it into a keyword.
    //
    // Exalted is the honest example left, and it is a different kind of gap: the decline is not the
    // grammar missing a sentence but `Keyword` missing a constant, which no rule here can fix.
    "text outside the grammar declines, and says where" {
        val declined = Grammar.abilityLine.parseLine("Exalted")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
        declined.reason shouldBe com.wingedsheep.assay.syntax.DeclineReason.NO_PARSE
    }

    "a mana symbol the SDK cannot express declines rather than throwing" {
        Grammar.abilityLine.parseLine("Cycling {S}").shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "every keyword rule can print what it parses" {
        // Guards the one failure mode a per-rule test cannot: a `match` half that quietly matches
        // nothing, which would show up on the corpus as a print mismatch far from its cause.
        val samples = listOf(
            "Flying", "Trample", "First strike", "Double strike", "Protection from red",
            "Hexproof from black", "Ward {1}", "Toxic 1", "Devour 2", "Casualty 1",
            "Affinity for artifacts", "Affinity for Lizards", "Conspire", "Flanking", "Increment",
            "Foretell {1}{U}", "Plot {2}{G}", "Disturb {1}{W}", "Evoke {3}{B}", "Emerge {6}{U}",
            "Miracle {W}", "Dash {1}{R}", "Warp {2}{U}", "Cleave {3}{U}{U}", "Harmonize {4}{G}",
            "Mayhem {1}{B}", "Ninjutsu {1}{U}", "Sneak {B}", "Web-slinging {2}{W}",
            "Disguise {1}{U}", "Offspring {2}", "Madness {1}{R}", "Hideaway 4", "Saddle 2",
            "Start your engines!", "Ascend", "Riot", "Bushido 1", "Modular 3", "Fading 5",
            "Vanishing 3", "Renown 1", "Fabricate 2", "Tribute 3", "Mobilize 1", "Firebending 2",
            "Rampage 2", "Absorb 1", "Afflict 3",
        )
        samples.forEach { roundTrips(it) }
    }
})
