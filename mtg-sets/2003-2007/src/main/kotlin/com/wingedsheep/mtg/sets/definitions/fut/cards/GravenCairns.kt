package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Graven Cairns
 * Land
 * {T}: Add {C}.
 * {B/R}, {T}: Add {B}{B}, {B}{R}, or {R}{R}.
 *
 * The filter-land shape (Cascade Bluffs): one mana ability per output combination, each costing
 * the hybrid {B/R} plus {T}.
 */
val GravenCairns = card("Graven Cairns") {
    manaCost = ""
    colorIdentity = "BR"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{B/R}, {T}: Add {B}{B}, {B}{R}, or {R}{R}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B/R}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B/R}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK) then Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B/R}"), Costs.Tap)
        effect = Effects.AddMana(Color.RED, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Anthony S. Waters"
        flavorText = "Shamans of Kar-Sengir claim that their sun sets because it can no longer bear the gaze of those pain-carved cliffs."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81e7d329-ef6a-45f3-82b6-37a3606c00bc.jpg?1783943089"
        ruling("2007-05-01", "\"{B/R}, {T}\" is the same as saying \"{B}, {T}\" or \"{R}, {T}\".")
    }
}
