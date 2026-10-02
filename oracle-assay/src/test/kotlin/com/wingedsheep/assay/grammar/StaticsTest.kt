package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Conditions as SdkConditions
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The aura band: "Enchant creature" and the statics the enchanted creature gets.
 *
 * The three round-trip cases are Holy Strength, Flight and Spectral Flight — the whole of each of
 * those cards. The two refusal cases are the ones that matter: a static whose `GroupFilter` is *not*
 * the aura default must not print as an aura's sentence, or the rule silently prints a lord.
 */
class StaticsTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    // Enchant is a keyword ability in CR 702.5 and a `TargetRequirement` in the SDK, which is why it
    // is here and not in Keywords. The requirement is the ordinary filtered target, so the whole of
    // Filters arrives with it.
    "the enchant line is the aura's attachment restriction" {
        fragment("Enchant creature") shouldBe CardFragment(
            script = CardScript(
                auraTarget = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Creature),
                    id = Targets.SLOT,
                )
            )
        )
        roundTrips("Enchant creature")
        roundTrips("Enchant land")
        roundTrips("Enchant creature you control")
        roundTrips("Enchant creature an opponent controls")
    }

    // Holy Strength. The golden omits `filter` entirely because the aura form *is* ModifyStats's
    // default, so the rule constructs it the same way.
    "the aura pump is the default-filtered ModifyStats the goldens carry" {
        fragment("Enchanted creature gets +1/+2.") shouldBe
            CardFragment(script = CardScript(staticAbilities = listOf(ModifyStats(1, 2))))
        roundTrips("Enchanted creature gets +1/+2.")
        roundTrips("Enchanted creature gets -3/-0.")
    }

    // Shackles. The untap lock is the DOESNT_UNTAP flag riding GrantKeyword's string field at the
    // aura default filter.
    "the untap-lock aura is the DOESNT_UNTAP grant at the aura default" {
        fragment("Enchanted creature doesn't untap during its controller's untap step.") shouldBe
            CardFragment(script = CardScript(staticAbilities = listOf(GrantKeyword(AbilityFlag.DOESNT_UNTAP.name))))
        roundTrips("Enchanted creature doesn't untap during its controller's untap step.")
        // A lock aimed anywhere but the attached creature is a different sentence.
        Grammar.abilityLine.printLine(
            CardFragment(
                script = CardScript(
                    staticAbilities = listOf(GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source())),
                ),
            ),
        ) shouldBe null
    }

    // Goblin Rock Sled. Conditional, so no longer a card flag: the same grant aimed at the source,
    // wrapped in the condition.
    "the conditional self untap lock wraps the source-aimed grant" {
        val line = "~ doesn't untap during your untap step if you control a Forest."
        val ability = fragment(line).script.staticAbilities.single().shouldBeInstanceOf<ConditionalStaticAbility>()
        ability.ability shouldBe GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source())
        roundTrips(line)
    }

    // Flight. GrantKeyword holds a String, so reading it back has to find the enum constant rather
    // than assume one.
    "the granted keyword is the whole simple-keyword vocabulary" {
        fragment("Enchanted creature has flying.") shouldBe
            CardFragment(script = CardScript(staticAbilities = listOf(GrantKeyword(Keyword.FLYING))))
        roundTrips("Enchanted creature has flying.")
        roundTrips("Enchanted creature has shroud.")
        roundTrips("Enchanted creature has double strike.")
    }

    // Spectral Flight: one sentence, two abilities — the qualityRun / Mana.alternatives shape a
    // third time. The list is the model; there is no compound type.
    "a pump joined to a grant is two static abilities from one sentence" {
        fragment("Enchanted creature gets +2/+2 and has flying.") shouldBe CardFragment(
            script = CardScript(
                staticAbilities = listOf(ModifyStats(2, 2), GrantKeyword(Keyword.FLYING))
            )
        )
        roundTrips("Enchanted creature gets +2/+2 and has flying.")
        roundTrips("Enchanted creature gets +1/+1 and has trample.")
    }

    // The fail-closed half, and the reason the `match` rules reconstruct rather than walk fields.
    // "Creatures you control get +1/+1." is the *same SDK type* with a real `GroupFilter`, and the
    // lord rules read it as its own sentence. The aura rule must not be the one that prints it — a
    // rule that looked only at the two bonuses would spell a lord's line as an aura's and lose the
    // whole clause, which is what this asserts.
    "a lord's pump prints as a lord's line and not as an aura's" {
        val lord = CardFragment(
            script = CardScript(
                staticAbilities = listOf(
                    ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature).youControl())
                )
            )
        )
        Grammar.abilityLine.printLine(lord) shouldBe "Creatures you control get +1/+1."
    }

    // …and the same for the grant, whose default filter is the same one.
    "a keyword granted to a group prints as a lord's line" {
        val anthem = CardFragment(
            script = CardScript(
                staticAbilities = listOf(
                    GrantKeyword(Keyword.FLYING, GroupFilter(GameObjectFilter.Creature).youControl())
                )
            )
        )
        Grammar.abilityLine.printLine(anthem) shouldBe "Creatures you control have flying."
    }

    // The aura's line is the *scoped* value, which no noun phrase can produce, so the two families
    // stay disjoint by their filter rather than by an ordering in the alternation.
    "an aura's pump still prints as an aura's line" {
        val aura = CardFragment(script = CardScript(staticAbilities = listOf(ModifyStats(1, 2))))
        Grammar.abilityLine.printLine(aura) shouldBe "Enchanted creature gets +1/+2."
    }

    // The noun is in the text and not in the model: `attachedCreature()` says "the thing this is
    // attached to" and nothing about creature-ness, so "Enchanted land" and "Enchanted creature"
    // would denote one value. Exactly one is spelled; the other declines rather than being
    // re-spelled into a sentence the card does not print.
    "a noun the grammar does not spell declines rather than being normalized away" {
        Grammar.abilityLine.parseLine("Enchanted land has flying.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The second static family: what a permanent may and may not do in combat. It is a *product* of a
    // subject and a restriction and it reaches the durational slot too, so it has a test file of its
    // own — see [CombatRestrictionsTest]. What stays here is the one sentence that is not part of
    // that product, because its variable part is a condition rather than a blocker.
    "the attack restriction reads its land type through the filter cascade" {
        fragment("~ can't block.") shouldBe
            CardFragment(script = CardScript(staticAbilities = listOf(CantBlock())))
        roundTrips("~ can't attack unless defending player controls an Island.")
        roundTrips("~ can't attack unless defending player controls a Swamp.")
    }

    // Threshold's condition, and the shape the hand-size comparison became when it got a second
    // member: one zone, one player, one spelled number, and which way the comparison points.
    "a zone-count condition compares the count the golden compares" {
        fragment("~ gets +3/+0 as long as there are seven or more cards in your graveyard.")
            .script.staticAbilities shouldBe listOf(
            ConditionalStaticAbility(
                ability = ModifyStats(3, 0, GroupFilter.source()),
                condition = Compare(
                    DynamicAmount.Count(Player.You, Zone.GRAVEYARD),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(7),
                ),
            )
        )
        roundTrips("~ gets +3/+0 as long as there are seven or more cards in your graveyard.")
    }

    // Delirium. The same graveyard, counted by distinct types through the facade the cards use;
    // "permanent types" is the sibling aggregation and the same sentence.
    "delirium is the graveyard type count the facade builds" {
        fragment(
            "~ gets +1/+1 as long as there are four or more card types among cards in your graveyard."
        ).script.staticAbilities shouldBe listOf(
            ConditionalStaticAbility(
                ability = ModifyStats(1, 1, GroupFilter.source()),
                condition = SdkConditions.Delirium(),
            )
        )
        roundTrips("~ gets +1/+1 as long as there are four or more card types among cards in your graveyard.")
        roundTrips("~ gets +1/+1 as long as there are four or more permanent types among cards in your graveyard.")
    }

    // The Doran family. Two rules and one flag: the qualifier is a clause inside the noun phrase
    // when `onlyWhenToughnessGreaterThanPower` is set and nothing at all when it is clear, which is
    // why the boolean is a row rather than a slot. The subject is a distributive singular — "each
    // creature you control … assigns … its toughness" — so it slots `Filters.filter`, unlike every
    // other group static in this file.
    "assigning damage by toughness is a group static over the distributive singular" {
        fragment(
            "Each creature you control with toughness greater than its power assigns combat " +
                "damage equal to its toughness rather than its power."
        ).script.staticAbilities shouldBe listOf(
            AssignDamageEqualToToughness(
                filter = GroupFilter.AllCreaturesYouControl,
                onlyWhenToughnessGreaterThanPower = true,
            )
        )
        // Doran, the Siege Tower — no controller clause and no qualifier.
        fragment("Each creature assigns combat damage equal to its toughness rather than its power.")
            .script.staticAbilities shouldBe listOf(
            AssignDamageEqualToToughness(
                filter = GroupFilter(GameObjectFilter.Creature),
                onlyWhenToughnessGreaterThanPower = false,
            )
        )
        roundTrips("Each creature assigns combat damage equal to its toughness rather than its power.")
        roundTrips(
            "Each creature you control assigns combat damage equal to its toughness rather than " +
                "its power."
        )
        roundTrips(
            "Each creature you control with toughness greater than its power assigns combat " +
                "damage equal to its toughness rather than its power."
        )
        // Bark of Doran: the attached subject fronts the qualifier and pronominalizes the subject,
        // which is why it is a constant on the whole default value rather than a third row.
        roundTrips(
            "As long as enchanted creature's toughness is greater than its power, it assigns " +
                "combat damage equal to its toughness rather than its power."
        )
    }

    // The quoted grant: everything inside the quotation marks is the grammar an ability line already
    // prints, so one rule inherits the whole triggered-ability vocabulary. `GrantTriggeredAbility`
    // is read by `TriggerDetector` and is not a layer effect, so it sits beside the pump as its own
    // top-level static rather than inside a `CompositeStaticAbility`.
    "a quoted triggered ability is granted to the attached creature" {
        val line = "Enchanted creature gets +1/+0 and has \"Whenever ~ deals combat damage, " +
            "create a Blood token.\""
        val abilities = fragment(line).script.staticAbilities
        abilities.size shouldBe 2
        abilities[0] shouldBe ModifyStats(1, 0)
        abilities[1].shouldBeInstanceOf<GrantTriggeredAbility>()
        roundTrips(line)
    }

    // …and its activated twin, over the same printed shape. The two are disjoint by what the quoted
    // text can be, so nothing here is left choosing.
    "a quoted activated ability is granted to the attached creature" {
        val line = "Enchanted creature has \"{T}: Draw a card.\""
        fragment(line).script.staticAbilities.single().shouldBeInstanceOf<GrantActivatedAbility>()
        roundTrips(line)
    }

    // The multi-layer lord. One printed ability across Layers 4, 6 and 7b, so the value is a
    // `CompositeStaticAbility` (CR 613.6) and not three statics that would each re-resolve their own
    // affected set. The keyword clause is what chooses between the two templates.
    "a multi-layer lord is one CompositeStaticAbility" {
        val bare = "Creatures you control have base power and toughness 6/6 and are Oozes in " +
            "addition to their other types."
        val composite = fragment(bare).script.staticAbilities.single()
            .shouldBeInstanceOf<CompositeStaticAbility>()
        composite.abilities.size shouldBe 2
        roundTrips(bare)

        val withKeyword = "Creatures you control with +1/+1 counters on them have base power and " +
            "toughness 4/4, have flying, and are Angels in addition to their other types."
        fragment(withKeyword).script.staticAbilities.single()
            .shouldBeInstanceOf<CompositeStaticAbility>().abilities.size shouldBe 3
        roundTrips(withKeyword)
    }

    // Every rule in the family can print what it parses — the meta-test each family gets, because a
    // `match` half that quietly matches nothing compiles, parses, and surfaces as a print mismatch
    // far from its cause.
    "every static rule prints what it parses" {
        val lines = listOf(
            "Enchanted creature gets +1/+2.",
            "Enchanted creature has flying.",
            "Enchanted creature gets +2/+2 and has flying.",
            "Enchanted creature has \"Whenever ~ attacks, draw a card.\"",
            "Enchanted creature gets +1/+0 and has \"{T}: Draw a card.\"",
            "Creatures you control have base power and toughness 6/6 and are Oozes in addition " +
                "to their other types.",
            "~ can't attack unless defending player controls an Island.",
            "Each creature you control with toughness greater than its power assigns combat " +
                "damage equal to its toughness rather than its power.",
        )
        lines.forEach { line -> Grammar.abilityLine.printLine(fragment(line)) shouldBe line }
    }

    // Sigardian Paladin's gate. The condition is turn *history* keyed on a counter kind, so the kind
    // is the slot and the noun ("a creature") is the condition's whole domain — the opposite split
    // from every other rule in `Conditions`, and the SDK type's own.
    "a counter-history condition slots the kind and fixes the noun" {
        fragment("~ has trample as long as you've put one or more +1/+1 counters on a creature this turn.")
            .script.staticAbilities shouldBe listOf(
            ConditionalStaticAbility(
                ability = com.wingedsheep.sdk.scripting.GrantKeyword(
                    com.wingedsheep.sdk.core.Keyword.TRAMPLE,
                    GroupFilter.source(),
                ),
                condition = com.wingedsheep.sdk.scripting.conditions
                    .PutCounterKindOnCreatureThisTurn(CounterType.PLUS_ONE_PLUS_ONE),
            )
        )
        roundTrips("~ has trample as long as you've put one or more +1/+1 counters on a creature this turn.")
        roundTrips("~ has flying as long as you've put one or more charge counters on a creature this turn.")
    }

    // The counter-kind leaf is gated on the SDK's own list, so a kind Magic does not have declines
    // rather than being invented — the "Elves" -> `Elve` class, in a condition.
    "a counter kind the SDK does not name declines" {
        Grammar.abilityLine
            .parseLine("~ has trample as long as you've put one or more sprocket counters on a creature this turn.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }
})
