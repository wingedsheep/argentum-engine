package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Deathcap Cultivator (Shadows over Innistrad #202)
 * {1}{G}
 * Creature — Human Druid
 * 2/1
 *
 * {T}: Add {B} or {G}.
 * Delirium — This creature has deathtouch as long as there are four or more card types among cards
 * in your graveyard.
 */
val DeathcapCultivator = card("Deathcap Cultivator") {
    manaCost = "{1}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 1
    oracleText = "{T}: Add {B} or {G}.\n" +
        "Delirium — This creature has deathtouch as long as there are four or more card types " +
        "among cards in your graveyard."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Karla Ortiz"
        flavorText = "The old hermit would never say what he fed his mushrooms, and people learned not to ask."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc4aee46-ac42-4ede-ad06-906e2955a9d3.jpg"
    }
}
