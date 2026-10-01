package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tablet of Compleation — Phyrexia: All Will Be One #245
 * {2} · Artifact · Rare
 *
 * {T}: Put an oil counter on this artifact.
 * {T}: Add {C}. Activate only if this artifact has two or more oil counters on it.
 * {1}, {T}: Draw a card. Activate only if this artifact has five or more oil counters on it.
 *
 * Both thresholds are [Conditions.SourceCounterCountAtLeast] read live at activation, so oil
 * counters from any source (proliferate, other oil-givers) count toward them.
 */
val TabletOfCompleation = card("Tablet of Compleation") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Put an oil counter on this artifact.\n" +
        "{T}: Add {C}. Activate only if this artifact has two or more oil counters on it.\n" +
        "{1}, {T}: Draw a card. Activate only if this artifact has five or more oil counters on it."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "Put an oil counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceCounterCountAtLeast(CounterType.OIL, 2))
        )
        effect = Effects.AddColorlessMana(1)
        description = "Add {C}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceCounterCountAtLeast(CounterType.OIL, 5))
        )
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Martin de Diego Sádaba"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9747e4b0-fcf9-4f1d-b990-2a3e461adfee.jpg?1783917984"
    }
}
