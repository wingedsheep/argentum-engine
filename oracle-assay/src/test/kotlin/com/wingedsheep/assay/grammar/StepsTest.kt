package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The first rules that produce a `CardScript` rather than a keyword — the start of the pipeline
 * family, and the point where the line model had to widen to [CardFragment].
 */
class StepsTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    "a bare draw is the spell effect and takes no target" {
        fragment("Draw two cards.") shouldBe
            CardFragment(script = CardScript(spellEffect = Effects.DrawCards(2)))
        roundTrips("Draw two cards.")
    }

    "the singular is its own rule, so it prints as an article rather than a number" {
        fragment("Draw a card.") shouldBe
            CardFragment(script = CardScript(spellEffect = Effects.DrawCards(1)))
        roundTrips("Draw a card.")
    }

    "a targeted draw declares the requirement and binds the effect to it" {
        fragment("Target player draws two cards.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.DrawCards(2, Targets.bound()),
                targetRequirements = listOf(Targets.player()),
            )
        )
        roundTrips("Target player draws two cards.")
        roundTrips("Target player draws a card.")
    }

    "every number word the vocabulary spells round-trips" {
        listOf("two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
            .forEach { roundTrips("Draw $it cards.") }
    }

    // Singular and plural must not overlap, or every draw card in the corpus reports AMBIGUOUS.
    // "Draw one cards." is the shape that would prove they do.
    "the plural rule refuses one, so exactly one surface form exists per count" {
        Grammar.abilityLine.parseLine("Draw one cards.").shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.parseLine("Draw one card.").shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "a count past the vocabulary declines rather than printing a form nobody uses" {
        Grammar.abilityLine.parseLine("Draw twenty cards.").shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.printLine(
            CardFragment(script = CardScript(spellEffect = Effects.DrawCards(20)))
        ) shouldBe null
    }

    // The fail-closed half of every `match`: a script carrying more than the rule inspected must not
    // print, or the extra content is dropped and the line still round-trips — reversible, wrong.
    "a script with content the rule did not inspect refuses to print" {
        val withExtra = CardFragment(
            script = CardScript(
                spellEffect = Effects.DrawCards(2),
                triggeredAbilities = emptyList(),
                cantBeCountered = true,
            )
        )

        Grammar.abilityLine.printLine(withExtra) shouldBe null
    }

    "a targeted draw and a bare draw are different models, not two spellings of one" {
        fragment("Draw two cards.") shouldBe
            CardFragment(script = CardScript(spellEffect = Effects.DrawCards(2)))
        (fragment("Target player draws two cards.") == fragment("Draw two cards.")) shouldBe false
    }

    "a keyword line and a spell line stay distinguishable in the same alternation" {
        fragment("Flying").script shouldBe CardScript.EMPTY
        fragment("Draw a card.").keywordAbilities shouldBe emptyList()
    }

    // Murder's golden, written by hand, is exactly this model — which is the point of the
    // differential: the grammar has to land on what a person wrote from the same sentence.
    "destroying a targeted permanent declares the requirement the card declares" {
        fragment("Destroy target creature.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.Destroy(Targets.bound()),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
            )
        )
        roundTrips("Destroy target creature.")
    }

    "every verb in the family round-trips over the filter vocabulary" {
        listOf(
            "Destroy target artifact.",
            "Destroy target artifact or enchantment.",
            "Destroy target creature or planeswalker.",
            "Destroy target nonland permanent.",
            "Exile target creature.",
            "Exile target permanent.",
            "Tap target creature.",
            "Untap target artifact.",
            "Return target creature to its owner's hand.",
            "Return target permanent to its owner's hand.",
        ).forEach { roundTrips(it) }
    }

    // The quantifier table: "target", "up to one target", "N target", "up to N target", "up to X
    // target". Every verb of the family gets every row, which is the property the table exists for —
    // before it, "tap up to three target creatures" was written and "destroy up to three" was not.
    "every quantifier reaches every verb of the family" {
        listOf("Destroy", "Exile", "Tap", "Untap").forEach { verb ->
            roundTrips("$verb target creature.")
            roundTrips("$verb up to one target creature.")
            roundTrips("$verb two target creatures.")
            roundTrips("$verb up to three target creatures.")
            roundTrips("$verb up to X target creatures.")
            roundTrips("$verb any number of target creatures.")
        }
    }

    // "Up to one" is one field on the requirement and one clause in the sentence: the count stays at
    // one and `optional` is what lets the spell be cast choosing nothing (CR 601.2c).
    "up to one is the singular requirement with optional set" {
        fragment("Destroy up to one target creature.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.Destroy(Targets.bound()),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature, optional = true)),
            )
        )
        roundTrips("Destroy up to one target creature.")
    }

    // …and a plural quantifier is exactly one that admits several targets, so the effect is written
    // once per chosen target rather than once against the requirement.
    "a plural quantifier iterates the effect over the chosen targets" {
        fragment("Exile up to two target creatures.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = ForEachTargetEffect(listOf(Effects.Exile(EffectTarget.ContextTarget(0)))),
                targetRequirements = listOf(
                    Targets.several(2, GameObjectFilter.Creature, optional = true)
                ),
            )
        )
        fragment("Exile two target creatures.").script.targetRequirements shouldBe
            listOf(Targets.several(2, GameObjectFilter.Creature, optional = false))
        fragment("Exile up to X target creatures.").script.targetRequirements shouldBe
            listOf(Targets.upToX(GameObjectFilter.Creature))
        fragment("Exile any number of target creatures.").script.targetRequirements shouldBe
            listOf(Targets.anyNumber(GameObjectFilter.Creature))
    }

    // Oracle prints the plural possessive both ways, 110 lines to 55. One rule, two spellings, and
    // the minority never prints — so Scapegoat's line survives as a variant rather than a decline.
    "the older plural possessive parses and never prints" {
        fragment("Return any number of target creatures you control to their owner's hand.") shouldBe
            fragment("Return any number of target creatures you control to their owners' hands.")
        Grammar.abilityLine.printLine(
            fragment("Return any number of target creatures you control to their owner's hand.")
        ) shouldBe "Return any number of target creatures you control to their owners' hands."
    }

    // The agreement reaches past the noun phrase, which is why the shape takes two templates: the
    // possessive after the target is singular for one creature and plural for several.
    "a possessive past the noun agrees with the quantifier" {
        listOf(
            "Return target creature to its owner's hand.",
            "Return up to one target creature to its owner's hand.",
            "Return up to two target creatures to their owners' hands.",
            "Put target creature on top of its owner's library.",
            "Put two target lands on top of their owners' libraries.",
        ).forEach { roundTrips(it) }
    }

    // Singular and plural must not overlap, or every quantified card in the corpus reports
    // AMBIGUOUS — the same property `Draw one cards.` proves for the counting rules. "Up to one" is
    // the singular row's, so the plural rows still refuse one.
    "the quantifier rows take disjoint counts and disjoint nouns" {
        listOf(
            "Destroy up to one target creatures.",
            "Destroy up to two target creature.",
            // Digits are Oracle's convention for damage and life, never for a target count.
            "Destroy up to 1 target creature.",
        ).forEach { Grammar.abilityLine.parseLine(it).shouldBeInstanceOf<ParseOutcome.Declined>() }
    }

    // Fail-closed, and here it is also what tells the rows apart: a count no row can spell, and an
    // `optional` flag the bare row does not say, must refuse to print rather than print a sentence
    // that drops the difference.
    "a requirement carrying more than its row spells refuses to print" {
        fun printed(requirement: TargetRequirement) =
            Grammar.abilityLine.printLine(
                CardFragment(
                    script = CardScript(
                        spellEffect = ForEachTargetEffect(listOf(Effects.Destroy(EffectTarget.ContextTarget(0)))),
                        targetRequirements = listOf(requirement),
                    )
                )
            )

        printed(Targets.several(2, GameObjectFilter.Creature, optional = true)) shouldBe
            "Destroy up to two target creatures."
        // Twenty is past the number vocabulary, which stops where Oracle's own convention does.
        printed(Targets.several(20, GameObjectFilter.Creature, optional = true)) shouldBe null
    }

    "the controller clause is a suffix on the model as well as on the sentence" {
        fragment("Destroy target creature you control.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.Destroy(Targets.bound()),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature.youControl())),
            )
        )
        roundTrips("Destroy target creature you control.")
        roundTrips("Destroy target creature an opponent controls.")
        roundTrips("Tap target artifact you control.")
    }

    // Lightning Bolt's golden, written by hand, is exactly this model.
    "damage to any target declares the requirement the burn spells declare" {
        fragment("~ deals 3 damage to any target.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.DealDamage(3, Targets.bound()),
                targetRequirements = listOf(Targets.any()),
            )
        )
        roundTrips("~ deals 3 damage to any target.")
        roundTrips("~ deals 2 damage to target creature.")
        roundTrips("~ deals 1 damage to target creature an opponent controls.")
        roundTrips("~ deals 5 damage to target player.")
        // Kindlespark Duo. "Target opponent" is its own requirement type rather than a filter on
        // "target player", which is why it is a row beside it.
        fragment("~ deals 1 damage to target opponent.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.DealDamage(1, Targets.bound()),
                targetRequirements = listOf(Targets.opponent()),
            )
        )
        roundTrips("~ deals 1 damage to target opponent.")
    }

    // Giant Growth's golden. `Duration.EndOfTurn` is ModifyStats's default, which is why the
    // sentence's own words are the only place the duration is spelled.
    "a pump spell round-trips over both the filter and the modifier vocabulary" {
        fragment("Target creature gets +3/+3 until end of turn.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.ModifyStats(3, 3, Targets.bound()),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
            )
        )
        listOf(
            "Target creature gets +3/+3 until end of turn.",
            "Target creature gets -2/-0 until end of turn.",
            "Target creature gets +0/+2 until end of turn.",
            "Target creature you control gets +1/+1 until end of turn.",
            "Target artifact creature gets +2/-1 until end of turn.",
        ).forEach { roundTrips(it) }
    }

    // The pump sentence is the second family to slot the quantifier table, and its verb agrees in
    // number: one creature "gets", several "each get". Second Breakfast prints the plural.
    "the pump sentence takes every quantifier, and its verb agrees in number" {
        fragment("Up to two target creatures each get +2/+1 until end of turn.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = ForEachTargetEffect(
                    listOf(Effects.ModifyStats(2, 1, EffectTarget.ContextTarget(0)))
                ),
                targetRequirements = listOf(Targets.several(2, GameObjectFilter.Creature, optional = true)),
            )
        )
        listOf(
            "Up to one target creature gets +2/+0 until end of turn.",
            "Up to two target creatures each get +2/+1 until end of turn.",
            "Up to three target creatures each get -1/-1 until end of turn.",
            "Up to X target creatures each get +1/+1 until end of turn.",
            "Two target creatures each get +1/+0 until end of turn.",
        ).forEach { roundTrips(it) }
        // The fronted spelling comes along, because it is the same rule with one word moved.
        fragment("Until end of turn, up to one target creature gets +2/+0.") shouldBe
            fragment("Up to one target creature gets +2/+0 until end of turn.")
    }

    // The keyword-grant sibling takes the same rows, and its verb agrees the same way: "gains" for
    // one, "each gain" for several. Phalanx Formation prints the plural.
    "the keyword grant takes every quantifier" {
        fragment("Any number of target creatures each gain double strike until end of turn.") shouldBe
            CardFragment(
                script = CardScript(
                    spellEffect = ForEachTargetEffect(
                        listOf(Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.ContextTarget(0)))
                    ),
                    targetRequirements = listOf(Targets.anyNumber(GameObjectFilter.Creature)),
                )
            )
        listOf(
            "Target creature gains flying until end of turn.",
            "Up to one target creature gains first strike and vigilance until end of turn.",
            "Two target creatures each gain flying until end of turn.",
            "Up to two target creatures each gain trample until end of turn.",
            "Up to X target creatures each gain haste until end of turn.",
            "Any number of target creatures each gain double strike until end of turn.",
        ).forEach { roundTrips(it) }
    }

    // The compound sentence is where the plural rows actually pay — every quantified line the corpus
    // prints for it is plural. Both halves are per-target, so the whole composite goes *inside* the
    // iteration rather than the iteration being split in two.
    "the pump-and-grant sentence puts the whole compound inside one iteration" {
        fragment("Up to two target creatures each get +1/+1 and gain lifelink until end of turn.") shouldBe
            CardFragment(
                script = CardScript(
                    spellEffect = ForEachTargetEffect(
                        listOf(
                            Effects.Composite(
                                listOf(
                                    Effects.ModifyStats(1, 1, EffectTarget.ContextTarget(0)),
                                    Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.ContextTarget(0)),
                                )
                            )
                        )
                    ),
                    targetRequirements = listOf(Targets.several(2, GameObjectFilter.Creature, optional = true)),
                )
            )
        listOf(
            "Target creature gets +4/+0 and gains trample until end of turn.",
            "Up to one target creature gets +1/+1 and gains lifelink until end of turn.",
            // Windborne Charge, Coordinated Assault, Rouse the Mob.
            "Two target creatures you control each get +2/+2 and gain flying until end of turn.",
            "Up to two target creatures each get +1/+0 and gain first strike until end of turn.",
            "Any number of target creatures each get +2/+0 and gain trample until end of turn.",
        ).forEach { roundTrips(it) }
        // The second verb takes no "each" of its own — the adverb attaches once to the pair — so the
        // doubled spelling is not a variant, it is not this sentence.
        Grammar.abilityLine
            .parseLine("Up to two target creatures each get +1/+1 and each gain lifelink until end of turn.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // Damage and counters take the *singular* rows only. Their plural is a different sentence —
    // "divided as you choose among …" and "on each of up to two target creatures" — so a plural row
    // here would read a distribute model as a sentence that means something else.
    "damage and counters take the singular quantifier rows and refuse the plural ones" {
        listOf(
            "~ deals 3 damage to up to one target creature.",
            "~ deals 5 damage to up to one target creature or planeswalker.",
            "Put a +1/+1 counter on up to one target creature.",
            "Put a -1/-1 counter on up to one target creature.",
            "Put two +1/+1 counters on up to one target creature you control.",
        ).forEach { roundTrips(it) }

        listOf(
            // Never printed: damage over several targets is "divided as you choose among …".
            "~ deals 3 damage to up to two target creatures.",
            "~ deals 3 damage to any number of target creatures.",
            // Never printed: the distribute sentence says "on each of up to two target creatures".
            "Put a +1/+1 counter on up to two target creatures.",
            "Put a +1/+1 counter on any number of target creatures.",
        ).forEach { Grammar.abilityLine.parseLine(it).shouldBeInstanceOf<ParseOutcome.Declined>() }
    }

    // The sign of a zero modifier is not in the model — `Fixed(0)` is `Fixed(0)` — so the printer
    // derives it from the other component, which is the rule the whole corpus follows. Getting this
    // wrong is a print mismatch on ~250 cards rather than a wrong reading, but the gate must be 0.
    "a zero modifier takes the sign of the component beside it" {
        fun printed(power: Int, toughness: Int) = Grammar.abilityLine.printLine(
            CardFragment(
                script = CardScript(
                    spellEffect = Effects.ModifyStats(power, toughness, Targets.bound()),
                    targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
                )
            )
        )

        printed(-2, 0) shouldBe "Target creature gets -2/-0 until end of turn."
        printed(0, -1) shouldBe "Target creature gets -0/-1 until end of turn."
        printed(1, 0) shouldBe "Target creature gets +1/+0 until end of turn."
        printed(0, 0) shouldBe "Target creature gets +0/+0 until end of turn."
    }

    "the counted verbs read digits, where the draw rules read number words" {
        listOf(
            "You gain 3 life.",
            "You lose 2 life.",
            "Target player gains 5 life.",
            "Target player loses 1 life.",
            "Scry 2.",
            "Surveil 1.",
        ).forEach { roundTrips(it) }
    }

    // The two conventions live in one text and must not borrow each other's leaf: Oracle writes
    // "draw two cards" and "you gain 2 life", never the reverse of either.
    "a life total spelled as a word, or a card count as a digit, declines" {
        Grammar.abilityLine.parseLine("You gain three life.").shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.parseLine("Draw 2 cards.").shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // Fail-closed the other way: a requirement carrying a restriction the phrase does not spell
    // must not print as though it did. `excludeSelf` is "other target creature", a different card.
    "a target requirement the phrase does not spell refuses to print" {
        val other = CardFragment(
            script = CardScript(
                spellEffect = Effects.Destroy(Targets.bound()),
                targetRequirements = listOf(
                    TargetPermanent(
                        filter = TargetFilter(GameObjectFilter.Creature, excludeSelf = true),
                        id = Targets.SLOT,
                    )
                ),
            )
        )

        Grammar.abilityLine.printLine(other) shouldBe null
    }

    // The mass effects: one iteration over a GroupFilter with the per-member effect written against
    // EffectTarget.Self. Four printed shapes for one model, which is why the templates are
    // enumerated and the group filter is Filters slotted whole.
    "a group effect is one iteration over a filter" {
        fragment("Creatures you control get +1/+1 until end of turn.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.ForEachInGroup(
                    com.wingedsheep.sdk.scripting.filters.unified.GroupFilter(
                        GameObjectFilter.Creature.youControl()
                    ),
                    Effects.ModifyStats(1, 1, com.wingedsheep.sdk.scripting.targets.EffectTarget.Self),
                )
            )
        )
        roundTrips("Creatures you control get +1/+1 until end of turn.")
        roundTrips("White creatures get +2/+0 until end of turn.")
        roundTrips("Creatures you control gain reach until end of turn.")
        roundTrips("Destroy all white creatures.")
        roundTrips("Untap all creatures you control.")
        roundTrips("Tap all other creatures.")
        roundTrips("~ deals 1 damage to each attacking creature.")
    }

    // `noRegenerate` is a field on the same iteration rather than a second effect, so the rule spans
    // both printed sentences and the plain sweep refuses to print a value carrying it.
    "a sweep that forbids regeneration is its own sentence pair" {
        roundTrips("Destroy all creatures. They can't be regenerated.")
        fragment("Destroy all creatures. They can't be regenerated.") shouldNotBe
            fragment("Destroy all creatures.")
    }

    // Both leaves in one file: a quantity of cards is a word, a quantity of life or damage a numeral.
    "the counted verbs keep the two number conventions apart" {
        roundTrips("You gain 3 life.")
        roundTrips("You gain 1 life for each Forest on the battlefield.")
        roundTrips("You gain 2 life for each Mountain target opponent controls.")
        roundTrips("~ deals X damage to any target.")
        roundTrips("Target creature gets +3/+3 and gains flying until end of turn.")
    }
})