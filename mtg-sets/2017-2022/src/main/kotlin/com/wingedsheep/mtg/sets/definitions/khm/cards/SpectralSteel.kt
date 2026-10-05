package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spectral Steel {1}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * {1}{W}, Exile this card from your graveyard: Return another target Aura or Equipment card
 * from your graveyard to your hand.
 */
val SpectralSteel = card("Spectral Steel") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2.\n" +
        "{1}{W}, Exile this card from your graveyard: Return another target Aura or Equipment card from your graveyard to your hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        val card = target(TargetFilter.CardInGraveyard.withAnySubtype("Aura", "Equipment").ownedByYou().other())
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf036489-ef9e-40ee-a1bb-24ee37c554f1.jpg?1783928275"
    }
}
