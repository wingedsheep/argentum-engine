package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vile Manifestation — Hour of Devastation #80
 * {1}{B} · Creature — Horror · 0/4
 *
 * This creature gets +1/+0 for each card with cycling in your graveyard.
 * Cycling {2}
 *
 * The bonus is a [GrantDynamicStats] scoped to the source, counting your graveyard with
 * `withCycling()` — the same count Zenith Flare uses, so typecycling cards count too.
 */
val VileManifestation = card("Vile Manifestation") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 0
    toughness = 4
    oracleText = "This creature gets +1/+0 for each card with cycling in your graveyard.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withCycling()),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Jason Felix"
        flavorText = "The glyphs writhed and twisted, spewing out dark, vengeful magic."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7160bc31-bdfa-4654-9d69-95ee2a5fe870.jpg?1783936034"
    }
}
