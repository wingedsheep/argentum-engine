package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ray of Enfeeblement — Adventures in the Forgotten Realms #116
 * {B} · Instant · Uncommon
 *
 * Target creature gets -4/-1 until end of turn. If that creature is white, it gets -4/-4 until
 * end of turn instead.
 *
 * The colour is checked on resolution, so the "instead" is an `If`/`otherwise` over the target.
 */
val RayOfEnfeeblement = card("Ray of Enfeeblement") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-1 until end of turn. If that creature is white, it gets -4/-4 until end of turn instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.WHITE), t),
            then = Effects.ModifyStats(-4, -4, t),
            otherwise = Effects.ModifyStats(-4, -1, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Campbell White"
        flavorText = "Erigan cried out in pain as a hideous weakness settled into his bones."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff5d948b-fd89-4b90-9b63-ad7da13b5f0e.jpg?1783926490"
    }
}
