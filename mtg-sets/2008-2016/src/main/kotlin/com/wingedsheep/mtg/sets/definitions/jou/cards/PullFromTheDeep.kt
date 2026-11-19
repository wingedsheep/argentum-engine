package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pull from the Deep
 * {2}{U}{U}
 * Sorcery
 *
 * Return up to one target instant card and up to one target sorcery card from your graveyard to
 * your hand. Exile Pull from the Deep.
 *
 * Shreds of Sanity's shape without the discard: two independent optional graveyard slots, each
 * returned to hand, and `selfExile()` for "Exile Pull from the Deep".
 */
val PullFromTheDeep = card("Pull from the Deep") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to one target instant card and up to one target sorcery card from your " +
        "graveyard to your hand. Exile Pull from the Deep."

    spell {
        selfExile()
        val instant = target(
            TargetFilter(GameObjectFilter.Instant.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        val sorcery = target(
            TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.ReturnToHand(instant) then Effects.ReturnToHand(sorcery)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Ryan Yee"
        flavorText = "Ideas, not stones, form the true foundation of Meletis."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a051b622-fa1d-4694-aef7-0fcdd7bff8d2.jpg?1783939444"
    }
}
