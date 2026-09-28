package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scrollshift
 * {2}{W}
 * Instant
 * Exile up to one target artifact, creature, or enchantment you control, then return it to the
 * battlefield under its owner's control.
 * Draw a card.
 *
 * "Up to one" makes the target optional: cast with no target, the blink does nothing and the spell
 * still resolves for the card draw. The returned permanent is a new object (CR 400.7).
 */
val Scrollshift = card("Scrollshift") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile up to one target artifact, creature, or enchantment you control, then return it " +
        "to the battlefield under its owner's control.\nDraw a card."

    spell {
        val permanent = target(
            TargetFilter(GameObjectFilter.ArtifactCreatureOrEnchantment.youControl()),
            optional = true
        )
        effect = Effects.Exile(permanent) then
            Effects.Move(permanent, Zone.BATTLEFIELD) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Bram Sels"
        flavorText = "Bim lashed out with the only weapon she'd never run out of: lecture notes."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fc278d1-2ea4-4fbc-95cd-a9cd48c3c630.jpg?1783917051"
    }
}
