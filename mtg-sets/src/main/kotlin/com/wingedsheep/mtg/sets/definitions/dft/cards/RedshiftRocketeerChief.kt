package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Redshift, Rocketeer Chief — Aetherdrift #218
 * {R}{G} · Legendary Creature — Goblin Pilot · 2/3
 *
 * Vigilance
 * {T}: Add X mana of any one color, where X is Redshift's power. Spend this mana only to activate
 * abilities.
 * Exhaust — {10}{R}{G}: Put any number of permanent cards from your hand onto the battlefield.
 *
 * The mana ability is [Effects.AddAnyColorMana] — *one* chosen color, X of it, not X mana in any
 * combination — with X read as [DynamicAmounts.sourcePower] so it tracks Redshift's **projected**
 * power (counters, lords, and any pump applied before the ability resolves all count). Tagged
 * [ManaRestriction.AbilityActivationOnly], the unqualified "only to activate abilities" restriction
 * (any activated ability of any source, matching Boommobile and Guidelight Optimizer) rather than an
 * artifact- or type-scoped one. That pool-level restriction constrains what each pip may pay for, not
 * how many abilities it feeds, so the mana may be split across several activations — and it
 * comfortably covers Redshift's own exhaust ability.
 *
 * The payoff is [Patterns.Hand.putFromHand] with `anyNumber = true`: Gather (hand, permanent cards) →
 * Select any number → Move to the battlefield. "Any number" includes zero, so declining is legal and
 * an empty hand is a no-op rather than a stuck decision. `isExhaust = true` makes the DSL add
 * [com.wingedsheep.sdk.scripting.ActivationRestriction.Once], which is per-object — a Redshift that
 * leaves and returns is a new object and may activate it again.
 */
val RedshiftRocketeerChief = card("Redshift, Rocketeer Chief") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Goblin Pilot"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "{T}: Add X mana of any one color, where X is Redshift's power. Spend this mana only to " +
        "activate abilities.\n" +
        "Exhaust — {10}{R}{G}: Put any number of permanent cards from your hand onto the " +
        "battlefield. (Activate each exhaust ability only once.)"

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            DynamicAmounts.sourcePower(),
            ManaRestriction.AbilityActivationOnly
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add X mana of any one color, where X is Redshift's power. Spend this " +
            "mana only to activate abilities."
    }

    activatedAbility {
        cost = Costs.Mana("{10}{R}{G}")
        isExhaust = true
        effect = Patterns.Hand.putFromHand(
            filter = Filters.Permanent,
            anyNumber = true,
            prompt = "Choose any number of permanent cards to put onto the battlefield"
        )
        description = "Put any number of permanent cards from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fba3820-32ba-47e9-9fa8-f985ff471b3f.jpg?1783907854"
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
        ruling(
            "2025-02-07",
            "If an ability triggers whenever you activate an exhaust ability, that ability resolves " +
                "before the exhaust ability resolves."
        )
    }
}
