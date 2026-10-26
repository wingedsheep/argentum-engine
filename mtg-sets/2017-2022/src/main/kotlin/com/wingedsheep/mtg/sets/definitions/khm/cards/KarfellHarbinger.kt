package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Karfell Harbinger (Kaldheim #65)
 * {1}{U} · Creature — Zombie Wizard 1/3
 * {T}: Add {U}. Spend this mana only to foretell a card from your hand or cast an instant or sorcery spell.
 *
 * `AnyOf(ForetellOnly, InstantOrSorceryOnly)`: the mana pays the foretell special action's {2}
 * (CR 702.143a) or any instant or sorcery spell — including a foretold one cast from exile.
 */
val KarfellHarbinger = card("Karfell Harbinger") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Wizard"
    power = 1
    toughness = 3
    oracleText = "{T}: Add {U}. Spend this mana only to foretell a card from your hand or cast an instant or sorcery spell."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.BLUE,
            restriction = ManaRestriction.AnyOf(
                listOf(ManaRestriction.ForetellOnly, ManaRestriction.InstantOrSorceryOnly)
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Josh Hass"
        flavorText = "\"This is a realm of the living. King Narfi's legion is coming to change that.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/4911016c-b92a-47cc-9553-b424d7be196a.jpg?1783928260"
    }
}
