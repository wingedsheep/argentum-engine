package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Nova Cleric
 * {W}
 * Creature — Human Cleric
 * 1/2
 * {2}{W}, {T}, Sacrifice Nova Cleric: Destroy all enchantments.
 */
val NovaCleric = card("Nova Cleric") {
    manaCost = "{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "{2}{W}, {T}, Sacrifice Nova Cleric: Destroy all enchantments."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap, Costs.SacrificeSelf)
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllEnchantments,
            effect = MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Alan Pollack"
        flavorText = "\"Our noblest thoughts are our very first and our very last.\""
        imageUri = "https://cards.scryfall.io/large/front/b/2/b2048d84-b5e6-405c-9091-1997a0c4e1a5.jpg?1562937263"
    }
}
