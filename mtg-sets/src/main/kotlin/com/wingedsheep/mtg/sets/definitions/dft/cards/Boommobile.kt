package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boommobile — Aetherdrift #113
 * {2}{R}{R} · Artifact — Vehicle · 5/5
 *
 * When this Vehicle enters, add four mana of any one color. Spend this mana only to activate
 * abilities.
 * Exhaust — {X}{2}{R}: This Vehicle deals X damage to any target. Put a +1/+1 counter on this
 * Vehicle.
 * Crew 2
 *
 * The ramp half is [Effects.AddAnyColorMana] — *one* chosen color, four of it, not four mana in any
 * combination — tagged [ManaRestriction.AbilityActivationOnly]. Per the Scryfall ruling that mana may
 * be split across several abilities, which is exactly what a pool-level restriction gives: it
 * constrains what each pip may be spent on, not how many abilities it feeds. It comfortably covers
 * the Vehicle's own exhaust ability (and its crew cost is free, so the mana is not stranded).
 *
 * The payoff is an ordinary X-cost activated ability with `isExhaust = true`, which the DSL turns
 * into "Activate only once" by adding [com.wingedsheep.sdk.scripting.ActivationRestriction.Once] —
 * per-object, so a Boommobile that leaves and returns is a new object and may fire again. X is read
 * back with [Effects.DealXDamage] (i.e. `DynamicAmount.XValue`). The counter is not conditional on the damage: it lands even if
 * the target became illegal, so both halves sit in one `Composite`.
 */
val Boommobile = card("Boommobile") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "When this Vehicle enters, add four mana of any one color. Spend this mana only to " +
        "activate abilities.\n" +
        "Exhaust — {X}{2}{R}: This Vehicle deals X damage to any target. Put a +1/+1 counter on " +
        "this Vehicle. (Activate each exhaust ability only once.)\n" +
        "Crew 2"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.AddAnyColorMana(4, ManaRestriction.AbilityActivationOnly)
        description = "When this Vehicle enters, add four mana of any one color. Spend this mana " +
            "only to activate abilities."
    }

    activatedAbility {
        cost = Costs.Mana("{X}{2}{R}")
        isExhaust = true
        val victim = target("any target", Targets.Any)
        effect = Effects.Composite(
            Effects.DealXDamage(victim),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "This Vehicle deals X damage to any target. Put a +1/+1 counter on this Vehicle."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Alexandr Leskinen"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/930c8289-4043-401a-8a7f-22349b7148b4.jpg?1783907886"
        ruling(
            "2025-02-07",
            "You may spend the four mana added by the first ability on the same ability or on multiple abilities."
        )
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
    }
}
