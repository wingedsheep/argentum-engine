package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tragic Poet
 * {W}
 * Creature — Human
 * 1/1
 * {T}, Sacrifice this creature: Return target enchantment card from your graveyard to your hand.
 */
val TragicPoet = card("Tragic Poet") {
    manaCost = "{W}"
    typeLine = "Creature — Human"
    power = 1
    toughness = 1
    oracleText = "{T}, Sacrifice this creature: Return target enchantment card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val t = target("target", TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Companion.Enchantment.ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        ))
        effect = MoveToZoneEffect(
            target = t,
            destination = Zone.HAND
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Anthony Palumbo"
        flavorText = "\"In a healing world I write—I, who will never be healed. Let my last gift be one of memory: from a thousand lost thoughts, choose one, and remember my name.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f957b353-7765-4c16-9645-d41000154130.jpg?1562746021"
    }
}
