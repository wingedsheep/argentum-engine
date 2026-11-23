package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Slaughterhorn — Gatecrash #134 (canonical printing)
 * {2}{G} · Creature — Beast · 3/2
 *
 * Bloodrush — {G}, Discard this card: Target attacking creature gets +3/+2 until end of turn.
 */
val Slaughterhorn = card("Slaughterhorn") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 2
    oracleText = "Bloodrush — {G}, Discard this card: Target attacking creature gets +3/+2 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(3, 2, creature)
        description = "Bloodrush — {G}, Discard this card: Target attacking creature gets +3/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Steve Prescott"
        flavorText = "The Orzhov clergy would never cross its path, but sending thrull couriers to do so is cheap and entertaining."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb3fcc7a-ff5b-4695-aa86-9166f6cba565.jpg?1783940115"
    }
}
