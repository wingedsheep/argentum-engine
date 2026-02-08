package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllEffect
import com.wingedsheep.sdk.scripting.GroupFilter

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

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap, Costs.SacrificeSelf)
        effect = DestroyAllEffect(GroupFilter.AllEnchantments)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Alan Pollack"
        flavorText = "\"Our noblest thoughts are our very first and our very last.\""
        imageUri = "https://cards.scryfall.io/large/front/b/2/b2048d84-b5e6-405c-9091-1997a0c4e1a5.jpg?1562937263"
    }
}
