package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Militant Inquisitor — Shadows over Innistrad #26
 * {2}{W} · Creature — Human Cleric · 2/3
 *
 * This creature gets +1/+0 for each Equipment you control.
 *
 * Counts every Equipment you control, attached or not — the same continuously recomputed
 * [GrantDynamicStats] on the source as [com.wingedsheep.mtg.sets.definitions.mrd.cards.NimShrieker],
 * filtered to the Equipment subtype.
 */
val MilitantInquisitor = card("Militant Inquisitor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "This creature gets +1/+0 for each Equipment you control."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Artifact.withSubtype("Equipment")
            ).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Karl Kopinski"
        flavorText = "\"I am an instrument of the church wielded against Avacyn's enemies.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68d99f1f-80d6-4b5e-aeae-83fb8ae62cdc.jpg?1783937816"
    }
}
