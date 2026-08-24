package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sustenance
 * {1}{G}
 * Enchantment
 * {1}, Sacrifice a land: Target creature gets +1/+1 until end of turn.
 */
val Sustenance = card("Sustenance") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{1}, Sacrifice a land: Target creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Land))
        val creature = target("target", Targets.Creature)
        effect = Effects.ModifyStats(1, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "278"
        artist = "Qiao Dafu"
        flavorText = "Like the dryads, the forest itself willingly gives up some of its life for the sake of the future."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a61db44-80dc-4058-9c9d-65cd18e63fd4.jpg"
    }
}
