package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Costs as SdkCosts
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The cost-colon-effect sentence, and the mana vocabulary that is nearly all of it — the rules that
 * make a land parse whole.
 */
class ActivatedTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun manaAbility(cost: AbilityCost, color: Color, amount: Int = 1) = ActivatedAbility(
        id = AbilityId("activated"),
        cost = cost,
        effect = Effects.AddMana(color, amount),
        timing = TimingRule.ManaAbility,
        isManaAbility = true,
    )

    // Llanowar Elves' golden, and 472 others: the whole model is cost, effect, and the two fields
    // that say it is a mana ability.
    "a tap-for-mana line is the ability a hand-written card carries" {
        fragment("{T}: Add {G}.") shouldBe CardFragment(
            script = CardScript(activatedAbilities = listOf(manaAbility(AbilityCost.Tap, Color.GREEN)))
        )
        roundTrips("{T}: Add {G}.")
    }

    // Jungle Hollow's golden is two CostTap entries, one BLACK and one GREEN — CR-shorthand for two
    // abilities, the same shape `Keywords.qualityRun` gives multi-quality protection.
    "a dual land's line is two abilities sharing one cost" {
        fragment("{T}: Add {B} or {G}.") shouldBe CardFragment(
            script = CardScript(
                activatedAbilities = listOf(
                    manaAbility(AbilityCost.Tap, Color.BLACK),
                    manaAbility(AbilityCost.Tap, Color.GREEN),
                )
            )
        )
        roundTrips("{T}: Add {B} or {G}.")
        roundTrips("{T}: Add {W}, {U}, or {B}.")
    }

    "the cost vocabulary round-trips over the shapes a mana ability prints" {
        listOf(
            "{T}: Add {C}.",
            "{T}: Add {G}{G}.",
            "{T}: Add {C}{C}{C}.",
            "{1}: Add {B}.",
            "{2}, {T}: Add {R}.",
            "{2}{R}, {T}: Add {W} or {U}.",
        ).forEach { roundTrips(it) }

        fragment("{2}, {T}: Add {R}.").script.activatedAbilities.single().cost shouldBe
            SdkCosts.Composite(SdkCosts.Mana("{2}"), AbilityCost.Tap)
    }

    // Nantuko Elder: a string of different symbols is one effect per run, chained in printed order.
    "a string of different mana symbols reads as the runs chained with then" {
        listOf(
            "{T}: Add {C}{G}.",
            "{T}, Sacrifice ~: Add {W}{B}.",
            "{T}: Add {G}{G}{W}.",
        ).forEach { roundTrips(it) }

        fragment("{T}: Add {C}{G}.").script.activatedAbilities.single().effect shouldBe
            (Effects.AddColorlessMana(1) then Effects.AddMana(Color.GREEN, 1))
        fragment("{T}: Add {G}{G}{W}.").script.activatedAbilities.single().effect shouldBe
            (Effects.AddMana(Color.GREEN, 2) then Effects.AddMana(Color.WHITE, 1))
    }

    // The whole reason the effect clause is a Steps slot: every step rule reaches this sentence
    // without this file knowing any of them.
    "the clause after the colon is the same grammar a spell prints" {
        listOf(
            "{T}: Draw a card.",
            "{2}: Target creature gets +1/+1 until end of turn.",
            "{1}, {T}: Tap target creature.",
            "{T}: You gain 2 life.",
        ).forEach { roundTrips(it) }

        fragment("{1}, {T}: Tap target creature.").script.activatedAbilities.single()
            .targetRequirements shouldBe listOf(Targets.permanent(GameObjectFilter.Creature))
    }

    // CR 605.1a: no target, it could add mana, and neither its cost nor its effect moves a card to
    // or from a library. All of it is derived rather than spelled, because no printed word says any
    // of it.
    "mana-ability-ness is derived from the effect, not from the sentence" {
        fragment("{T}: Add {G}.").script.activatedAbilities.single().isManaAbility shouldBe true
        fragment("{T}: Draw a card.").script.activatedAbilities.single().isManaAbility shouldBe false
        fragment("{T}: Draw a card.").script.activatedAbilities.single().timing shouldBe
            TimingRule.InstantSpeed
    }

    // The clause CR 605.1a gained on August 7, 2026. Nothing in either printed line changed on that
    // date, which is the whole reason the derivation has to carry the rule: Chromatic Sphere's own
    // Gatherer ruling still calls it a mana ability, and it is not one any more.
    "a library rider disqualifies a mana ability, on the effect or on the cost" {
        val sphere = fragment("{T}: Add {G}. Draw a card.").script.activatedAbilities.single()
        sphere.isManaAbility shouldBe false
        sphere.timing shouldBe TimingRule.InstantSpeed

        val assistant = fragment("{T}, Mill a card: Add {C}.").script.activatedAbilities.single()
        assistant.isManaAbility shouldBe false
        assistant.timing shouldBe TimingRule.InstantSpeed
    }

    // Surveil takes a card off the library too, and the linter's mirror rule says so — the two
    // derivations have to agree card-for-card or the differential gate reports rule drift as a card
    // defect. A scry rider, which reorders within the library, still leaves the classification alone.
    "a surveil rider disqualifies a mana ability, a scry rider does not" {
        fragment("{T}: Add {G}. Surveil 1.").script.activatedAbilities.single()
            .isManaAbility shouldBe false
        fragment("{T}: Add {G}. Scry 1.").script.activatedAbilities.single()
            .isManaAbility shouldBe true
    }

    // The fail-closed half. An `ActivatedAbility` has two dozen fields the sentence does not spell,
    // and printing one that carries any of them would drop it and still round-trip.
    "an ability carrying content the sentence does not spell refuses to print" {
        val restricted = CardFragment(
            script = CardScript(
                activatedAbilities = listOf(
                    manaAbility(AbilityCost.Tap, Color.GREEN)
                        .copy(restrictions = listOf(ActivationRestriction.Once))
                )
            )
        )
        Grammar.abilityLine.printLine(restricted) shouldBe null

        val relabelled = CardFragment(
            script = CardScript(
                activatedAbilities = listOf(
                    manaAbility(AbilityCost.Tap, Color.GREEN).copy(descriptionOverride = "{T}: Add {G}.")
                )
            )
        )
        Grammar.abilityLine.printLine(relabelled) shouldBe null
    }

    // The differential's answer for these is `ManaColorSet.Specific`, which the grammar deliberately
    // never emits — registering both would be one text with two models.
    "the choice form is several abilities and never a colour set" {
        fragment("{T}: Add {B} or {G}.").script.activatedAbilities.map { it.effect } shouldBe
            listOf(Effects.AddMana(Color.BLACK), Effects.AddMana(Color.GREEN))
    }

    // Shivan Reef's golden: each colour is its own ability, and each carries the damage rider.
    "a painland's rider follows every ability the choice denotes" {
        val rider = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        fragment("{T}: Add {U} or {R}. ~ deals 1 damage to you.").script.activatedAbilities.map { it.effect } shouldBe
            listOf(Effects.AddMana(Color.BLUE) then rider, Effects.AddMana(Color.RED) then rider)
        fragment("{T}: Add {U} or {R}. ~ deals 1 damage to you.").script.activatedAbilities
            .all { it.isManaAbility && it.timing == TimingRule.ManaAbility } shouldBe true
        roundTrips("{T}: Add {U} or {R}. ~ deals 1 damage to you.")
        roundTrips("{T}: Add {W}, {U}, or {B}. ~ deals 1 damage to you.")
    }

    // One printed "target" would become one target slot per ability, and two abilities with different
    // riders were never printed as one line.
    "a choice rider that targets declines, and unequal riders refuse to print" {
        Grammar.abilityLine.parseLine("{T}: Add {U} or {R}. ~ deals 1 damage to any target.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
        val painland = fragment("{T}: Add {U} or {R}. ~ deals 1 damage to you.")
        val (blue, red) = painland.script.activatedAbilities
        val unequal = painland.copy(
            script = painland.script.copy(
                activatedAbilities = listOf(
                    blue,
                    red.copy(effect = Effects.AddMana(Color.RED) then Effects.DealDamage(2, EffectTarget.PlayerRef(Player.You))),
                )
            )
        )
        Grammar.abilityLine.printLine(unequal) shouldBe null
    }

    // Dark Ritual: producing mana is a spell effect in its own right, which is why the rule lives in
    // Steps rather than only behind a cost.
    "adding mana is a spell line as well as an ability's effect" {
        fragment("Add {B}{B}{B}.") shouldBe
            CardFragment(script = CardScript(spellEffect = Effects.AddMana(Color.BLACK, 3)))
        roundTrips("Add {B}{B}{B}.")
    }
})
