package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zhur-Taa Swine — Gatecrash #210 (canonical printing)
 * {3}{R}{G} · Creature — Boar · 5/4
 *
 * Bloodrush — {1}{R}{G}, Discard this card: Target attacking creature gets +5/+4 until end of turn.
 */
val ZhurTaaSwine = card("Zhur-Taa Swine") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Boar"
    power = 5
    toughness = 4
    oracleText = "Bloodrush — {1}{R}{G}, Discard this card: Target attacking creature gets +5/+4 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(5, 4, creature)
        description = "Bloodrush — {1}{R}{G}, Discard this card: Target attacking creature gets +5/+4 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Yeong-Hao Han"
        flavorText = "Gurley was the first to domesticate one, though his widow didn't take much solace in that accomplishment."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cef93050-2f24-4c85-a00b-796e53868ac1.jpg?1783940097"
    }
}
