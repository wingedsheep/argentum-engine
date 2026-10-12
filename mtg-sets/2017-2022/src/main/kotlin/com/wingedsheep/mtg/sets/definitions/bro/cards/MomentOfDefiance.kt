package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Moment of Defiance
 * {2}{B}
 * Instant
 * Target creature gets +2/+1 and gains lifelink until end of turn.
 * Draw a card.
 */
val MomentOfDefiance = card("Moment of Defiance") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+1 and gains lifelink until end of turn.\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 1, t) then
            Effects.GrantKeyword(Keyword.LIFELINK, t) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Inkognit"
        flavorText = "\"Well? What are you waiting for? Let's not keep the buzzards waiting.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e99fb27-a1e9-491f-ab2a-3ccb493168cb.jpg?1783920083"
    }
}
