package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EntersWithKeywords
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** "This land enters tapped." — the self-replacement on a permanent's own entry. */
class ReplacementsTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    fun choice(line: String): EntersWithChoice =
        fragment(line).script.replacementEffects.single().shouldBeInstanceOf<EntersWithChoice>()

    // 234 hand-written cards carry exactly this, the bare default.
    "the tapped-entry line is the replacement effect the goldens carry" {
        fragment("~ enters tapped.") shouldBe
            CardFragment(script = CardScript(replacementEffects = listOf(EntersTapped())))
        roundTrips("~ enters tapped.")
    }

    // Steam Vents and the other shock lands: the same type with one field set, which is why it is a
    // row beside the plain rule rather than a family of its own.
    "the shock-land sentence is the same type with a life cost" {
        fragment("As ~ enters, you may pay 2 life. If you don't, it enters tapped.") shouldBe
            CardFragment(script = CardScript(replacementEffects = listOf(EntersTapped(payLifeCost = 2))))
        roundTrips("As ~ enters, you may pay 2 life. If you don't, it enters tapped.")
    }

    // The check lands. The condition is a slot rather than a rule, so the whole of `Conditions`
    // arrives at once — the four sentences below are one template with four fillings.
    "a conditional tapped entry is the type's third field, filled from the condition vocabulary" {
        fragment("~ enters tapped unless you control a basic land.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersTapped(unlessCondition = Conditions.YouControl(GameObjectFilter.BasicLand))
                )
            )
        )
        roundTrips("~ enters tapped unless you control a basic land.")
        roundTrips("~ enters tapped unless a player has 13 or less life.")
        roundTrips("~ enters tapped unless you control two or more basic lands.")
    }

    // Two articles make two noun phrases with the verb elided, which is a disjunction of
    // *conditions*; one article makes one noun phrase, which is a disjunction inside the filter.
    // The goldens draw the line in the same place — Sulfur Falls holds `Any(Exists, Exists)`.
    "the check lands' two-article disjunction is a condition, not a filter" {
        fragment("~ enters tapped unless you control an Island or a Mountain.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersTapped(
                        unlessCondition = Conditions.Any(
                            Conditions.YouControl(GameObjectFilter.Land.withSubtype("Island")),
                            Conditions.YouControl(GameObjectFilter.Land.withSubtype("Mountain")),
                        )
                    )
                )
            )
        )
        roundTrips("~ enters tapped unless you control an Island or a Mountain.")
    }

    // "Other" is `AggregateBattlefield.excludeSelf`, not one-higher arithmetic over the whole
    // group: the two agree only while the source itself matches the filter. Twenty hand-written
    // lands spelled the shortcut and moved to this reading in the change that added the rule.
    "the fast and slow lands count OTHER lands, and the word is a field on the amount" {
        fragment("~ enters tapped unless you control two or fewer other lands.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersTapped(
                        unlessCondition = Conditions.YouControlOtherAtMost(2, GameObjectFilter.Land)
                    )
                )
            )
        )
        roundTrips("~ enters tapped unless you control two or fewer other lands.")
        roundTrips("~ enters tapped unless you control two or more other lands.")
        roundTrips("~ enters tapped unless you control three or more other Islands.")
    }

    // The reconstruct-and-compare in the `match` half: a value carrying a field this sentence has
    // no room for refuses to print rather than dropping it.
    "a tapped entry that also costs life refuses to print as the conditional sentence" {
        val both = CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersTapped(unlessCondition = IsYourTurn, payLifeCost = 2)
                )
            )
        )
        Grammar.abilityLine.printLine(both) shouldBe null
    }

    // A condition the SDK cannot name still declines, and is counted rather than approximated —
    // "you have two or more opponents" is ten corpus cards with no facade to build through. The turn
    // clause declines for the opposite reason: the SDK names it, and `SpellCosts.leadingGate`
    // already owns its one printed form.
    "a condition outside the vocabulary declines rather than losing itself" {
        Grammar.abilityLine.parseLine("~ enters tapped unless you have two or more opponents.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.parseLine("~ enters tapped unless it's your turn.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // ---------------------------------------------------------------------------------------
    // "As ~ enters, choose …" — `EntersWithChoice` as the product it calls itself
    // ---------------------------------------------------------------------------------------

    // The kind of choice is a noun phrase, so each is a row; the goldens are the assertion that the
    // row picked the right `ChoiceType` (The Rack, Phantasmal Terrain, Dauntless Bodyguard).
    "the kind of choice is the noun phrase the card prints" {
        choice("As ~ enters, choose a color.") shouldBe EntersWithChoice(ChoiceType.COLOR)
        choice("As ~ enters, choose an opponent.") shouldBe EntersWithChoice(ChoiceType.OPPONENT)
        choice("As ~ enters, choose a basic land type.") shouldBe
            EntersWithChoice(ChoiceType.BASIC_LAND_TYPE)
        choice("As ~ enters, choose another creature you control.") shouldBe
            EntersWithChoice(ChoiceType.CREATURE_ON_BATTLEFIELD)
    }

    // The three `CardNamePool` values are three noun phrases rather than an adjective slot the
    // widest one would have to leave empty. Nevermore, Gideon's Intervention, Petrified Hamlet.
    "the card-name pool is a word in the noun phrase" {
        choice("As ~ enters, choose a card name.") shouldBe
            EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = CardNamePool.ANY)
        choice("As ~ enters, choose a nonland card name.") shouldBe
            EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = CardNamePool.NONLAND)
        choice("As ~ enters, choose a land card name.") shouldBe
            EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = CardNamePool.LAND)
    }

    // Sorcerous Spyglass. The look and the wider pool are spelled together on every corpus line that
    // has either, so this is one sentence with the flag set rather than a prefix on the plain rule.
    "the look-at-hand clause is the flag on the same value" {
        choice("As ~ enters, look at an opponent's hand, then choose any card name.") shouldBe
            EntersWithChoice(
                choiceType = ChoiceType.CARD_NAME,
                cardNamePool = CardNamePool.ANY,
                lookAtOpponentHand = true,
            )
        roundTrips("As ~ enters, look at an opponent's hand, then choose any card name.")
    }

    // Callous Oppressor. One word position, so one slot rather than a second row per noun.
    "who chooses is a slot the whole noun vocabulary shares" {
        choice("As ~ enters, an opponent chooses a creature type.") shouldBe
            EntersWithChoice(ChoiceType.CREATURE_TYPE, chooser = Player.AnOpponent)
        roundTrips("As ~ enters, an opponent chooses a creature type.")
        roundTrips("As ~ enters, an opponent chooses a color.")
    }

    // Shapeshifter and Talion: CR 614.1c's chosen number is the one `ChoiceType` whose sentence
    // carries data past the kind of choice, and the two numbers are the SDK's two fields.
    "a chosen number's bounds are the two numerals the sentence prints" {
        choice("As ~ enters, choose a number between 0 and 7.") shouldBe
            EntersWithChoice(ChoiceType.NUMBER, minValue = 0, maxValue = 7)
        roundTrips("As ~ enters, choose a number between 0 and 7.")
        roundTrips("As ~ enters, choose a number between 1 and 10.")
    }

    // The band's three write-offs, asserted so they stay declines rather than drifting into a
    // half-reading. `MODE` carries an `id`, a `description` and an `iconKey` the sentence does not
    // contain; an unbounded number has nothing to say about `minValue`/`maxValue`; and the reveal
    // lands want a field `EntersTapped` does not have.
    "the band's write-offs decline rather than being approximated" {
        declines("As ~ enters, choose Khans or Dragons.")
        declines("As ~ enters, choose a number.")
        declines("As ~ enters, you may reveal a Faerie card from your hand. If you don't, ~ enters tapped.")
    }

    // The fail-closed half. `chooser` names the only two players an as-it-enters line can, so a
    // value carrying a third refuses to print rather than losing the word — and a `MODE` value has
    // no noun phrase at all.
    "a choice the vocabulary cannot spell refuses to print" {
        listOf(
            EntersWithChoice(ChoiceType.COLOR, chooser = Player.EachOpponent),
            EntersWithChoice(ChoiceType.CREATURE_TYPE, allowedCreatureTypes = listOf("Elf")),
            EntersWithChoice(ChoiceType.MODE, modeOptions = listOf(ModeOption("khans", "Khans"))),
        ).forEach { value ->
            Grammar.abilityLine.printLine(
                CardFragment(script = CardScript(replacementEffects = listOf(value)))
            ) shouldBe null
        }
    }

    // Heron of Hope, Leyline of Hope, Angel of Vitality: the additive form, spelled the way
    // `ModifyLifeGain.description` spells it.
    "the life-gain replacement is the type's two arithmetic fields" {
        fragment("If you would gain life, you gain that much life plus 1 instead.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    ModifyLifeGain(
                        multiplier = 1,
                        modifier = 1,
                        appliesTo = EventPattern.LifeGainEvent(player = Player.You),
                    )
                )
            )
        )
        roundTrips("If you would gain life, you gain that much life plus 1 instead.")
        roundTrips("If you would gain life, you gain twice that much life instead.")
    }

    // The two spellings the SDK has for "gains no life" — this family builds neither, so a zeroed
    // `ModifyLifeGain` refuses to print rather than colliding with `PreventLifeGain`. And a value
    // carrying a `restrictions` gate (Phial of Galadriel) has nowhere to put it.
    "a zeroed or gated life-gain modification refuses to print" {
        listOf(
            ModifyLifeGain(multiplier = 0, modifier = 0, appliesTo = EventPattern.LifeGainEvent(Player.You)),
            ModifyLifeGain(
                multiplier = 2,
                appliesTo = EventPattern.LifeGainEvent(Player.You),
                restrictions = listOf(Conditions.LifeAtMost(5)),
            ),
            ModifyLifeGain(multiplier = 2, appliesTo = EventPattern.LifeGainEvent(Player.Each)),
        ).forEach { value ->
            Grammar.abilityLine.printLine(
                CardFragment(script = CardScript(replacementEffects = listOf(value)))
            ) shouldBe null
        }
    }

    // The counters-per-count family. The printed numeral is a *multiplier* over a battlefield
    // tally, which `Amounts.scaled` lowers to the bare aggregate at 1 and a `Multiply` above it —
    // so the article row and the numeral row take disjoint halves of the model and printing is
    // decided by the value rather than by the alternation's order.
    "entering counters can be a rate over a battlefield tally" {
        fragment("~ enters with a +1/+1 counter on it for each other creature you control.")
            .script.replacementEffects shouldBe listOf(
            EntersWithDynamicCounters(
                count = DynamicAmount.AggregateBattlefield(
                    player = Player.You,
                    filter = GameObjectFilter.Creature,
                    excludeSelf = true,
                ),
            )
        )
        // Hamlet Vanguard: "two … for each" is the multiplier, and "other" is the tally's own field
        // rather than a filter predicate.
        fragment("~ enters with two +1/+1 counters on it for each other nontoken Human you control.")
            .script.replacementEffects shouldBe listOf(
            EntersWithDynamicCounters(
                count = DynamicAmount.Multiply(
                    DynamicAmount.AggregateBattlefield(
                        player = Player.You,
                        filter = GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN).nontoken(),
                        excludeSelf = true,
                    ),
                    2,
                ),
            )
        )
        // Every row is an alternate spelling: Oracle prints the same model as "…X counters on it,
        // where X is the number of …" too, and that rule can print the whole `Amounts.count` domain
        // while this family reaches only the battlefield tallies. So these parse and come back as
        // variants — the model survives, the spelling normalizes onto the printer that covers more.
        Grammar.abilityLine.printLine(
            fragment("~ enters with a hope counter on it for each creature you control.")
        ) shouldBe "~ enters with X hope counters on it, where X is the number of creatures you control."

        fragment("~ enters with a time counter on it for each Island you control.")
            .script.replacementEffects.single().shouldBeInstanceOf<EntersWithDynamicCounters>()
        fragment("~ enters with three +1/+1 counters on it for each artifact you control.")
            .script.replacementEffects.single().shouldBeInstanceOf<EntersWithDynamicCounters>()
        fragment("~ enters with a +1/+1 counter on it for each creature on the battlefield.")
            .script.replacementEffects.single().shouldBeInstanceOf<EntersWithDynamicCounters>()
    }

    // The second line every disturb back face prints (CR 702.146b) — 28 cards, all of them MID/VOW
    // transforming cards. A `constant` on the whole value: the sentence spells no field beyond the
    // destination, and the sibling lines that name a filter instead are a different value.
    "the disturb back face's exile-instead line is a self-scoped redirect" {
        fragment("If ~ would be put into a graveyard from anywhere, exile it instead.") shouldBe
            CardFragment(
                script = CardScript(
                    replacementEffects = listOf(
                        RedirectZoneChange(
                            newDestination = Zone.EXILE,
                            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
                            selfOnly = true,
                        )
                    )
                )
            )
        roundTrips("If ~ would be put into a graveyard from anywhere, exile it instead.")
        // Rest in Peace and Dryad Militant name a filter rather than the source, so they are a
        // different value (`selfOnly = false`) and stay declined rather than folding into this rule.
        declines("If a card or token would be put into a graveyard from anywhere, exile it instead.")
        declines(
            "If an instant or sorcery card would be put into a graveyard from anywhere, " +
                "exile it instead."
        )
    }

    // Ardent Soldier, Kavu Titan: the goldens spell the keyword half as a second replacement beside
    // the counters, both gated on `WasKicked` — so one sentence is a two-effect line.
    "the kicker entry is counters gated on WasKicked, plus a keyword replacement when it says one" {
        fragment("If ~ was kicked, it enters with a +1/+1 counter on it.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersWithCounters(
                        counterType = CounterType.PLUS_ONE_PLUS_ONE,
                        count = 1,
                        selfOnly = true,
                        condition = Conditions.WasKicked,
                    )
                )
            )
        )
        fragment("If ~ was kicked, it enters with three +1/+1 counters on it and with trample.") shouldBe
            CardFragment(
                script = CardScript(
                    replacementEffects = listOf(
                        EntersWithCounters(
                            counterType = CounterType.PLUS_ONE_PLUS_ONE,
                            count = 3,
                            selfOnly = true,
                            condition = Conditions.WasKicked,
                        ),
                        EntersWithKeywords(
                            keywords = listOf(Keyword.TRAMPLE),
                            selfOnly = true,
                            condition = Conditions.WasKicked,
                        ),
                    )
                )
            )
        listOf(
            "If ~ was kicked, it enters with a +1/+1 counter on it.",
            "If ~ was kicked, it enters with five +1/+1 counters on it.",
            "If ~ was kicked, it enters with a +1/+1 counter on it and with haste.",
            "If ~ was kicked, it enters with two +1/+1 counters on it and with first strike.",
        ).forEach(::roundTrips)
        // The unconditional sentence is the plain rule's, and a kicked value never prints as it.
        Grammar.abilityLine.printLine(fragment("~ enters with two +1/+1 counters on it.")) shouldBe
            "~ enters with two +1/+1 counters on it."
    }

    // A value the sentence has no room for refuses to print rather than dropping the field.
    "a kicker entry carrying anything else refuses to print" {
        val kicked = EntersWithCounters(count = 2, selfOnly = true, condition = Conditions.WasKicked)
        listOf(
            listOf<com.wingedsheep.sdk.scripting.ReplacementEffect>(kicked.copy(selfOnly = false)),
            listOf(kicked.copy(condition = Conditions.WasBargained)),
            listOf(kicked, EntersWithKeywords(listOf(Keyword.FLYING), condition = Conditions.WasKicked)),
            listOf(
                kicked,
                EntersWithKeywords(listOf(Keyword.FLYING, Keyword.HASTE), selfOnly = true, condition = Conditions.WasKicked),
            ),
        ).forEach { effects ->
            Grammar.abilityLine.unparse(CardFragment.of(CardScript(replacementEffects = effects))) shouldBe null
        }
    }

    // Goblin Boarders, Blacksnag Buzzard, the Myojin: the plain entry with its `condition` set.
    "a trailing if-clause on a self-entry is the condition field" {
        fragment("~ enters with a +1/+1 counter on it if you attacked this turn.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersWithCounters(count = 1, selfOnly = true, condition = Conditions.YouAttackedThisTurn)
                )
            )
        )
        fragment("~ enters with a divinity counter on it if you cast it from your hand.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersWithCounters(
                        counterType = CounterType.DIVINITY,
                        count = 1,
                        selfOnly = true,
                        condition = Conditions.WasCastFromHand,
                    )
                )
            )
        )
        fragment("~ enters with two +1/+1 counters on it if you've cast two or more spells this turn.")
            .script.replacementEffects.single() shouldBe
            EntersWithCounters(count = 2, selfOnly = true, condition = Conditions.YouCastSpellsThisTurn(2))
        listOf(
            "~ enters with a +1/+1 counter on it if you attacked this turn.",
            "~ enters with a +1/+1 counter on it if a creature died this turn.",
            "~ enters with a +1/+1 counter on it if an opponent lost life this turn.",
            "~ enters with a +1/+1 counter on it if you control a permanent with mana value 4 or greater.",
            "~ enters with two +1/+1 counters on it if you've cast two or more spells this turn.",
            "~ enters with two -1/-1 counters on it if you cast it from your hand.",
            "~ enters with eight revival counters on it if you cast it.",
        ).forEach(::roundTrips)
    }

    // The kicker sentence fronts its condition and owns `WasKicked`; "it's bargained" is a cost-position
    // tense. Neither is printed as a trailing clause, and the unconditional rule never takes a condition.
    "a self-entry condition the trailing clause does not print refuses" {
        Grammar.abilityLine.printLine(fragment("If ~ was kicked, it enters with two +1/+1 counters on it.")) shouldBe
            "If ~ was kicked, it enters with two +1/+1 counters on it."
        declines("~ enters with two +1/+1 counters on it if it was kicked.")
        declines("~ enters with two +1/+1 counters on it if it's bargained.")
    }

    "every as-it-enters choice rule prints what it parses" {
        listOf(
            "As ~ enters, choose a color.",
            "As ~ enters, choose a creature type.",
            "As ~ enters, choose another creature you control.",
            "As ~ enters, choose a basic land type.",
            "As ~ enters, choose an opponent.",
            "As ~ enters, choose a land card name.",
            "As ~ enters, choose a nonland card name.",
            "As ~ enters, choose a card name.",
            "As ~ enters, an opponent chooses a card name.",
            "As ~ enters, choose a number between 0 and 7.",
            "As ~ enters, look at an opponent's hand, then choose any card name.",
        ).forEach { line -> Grammar.abilityLine.printLine(fragment(line)) shouldBe line }
    }
})
