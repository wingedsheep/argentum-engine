package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hall of Heliod's Generosity — Modern Horizons #241
 * Legendary Land
 *
 * {T}: Add {C}.
 * {1}{W}, {T}: Put target enchantment card from your graveyard on top of your library.
 */
val HallOfHeliodsGenerosity = card("Hall of Heliod's Generosity") {
    colorIdentity = "W"
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}{W}, {T}: Put target enchantment card from your graveyard on top of your library."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val t = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOnTopOfLibrary(t)
        description = "{1}{W}, {T}: Put target enchantment card from your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Daniel Ljunggren"
        flavorText = "The stronghold of Theros's light."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5cbd10a-b9a6-4c00-8280-72bb4add4390.jpg?1783933066"
    }
}
