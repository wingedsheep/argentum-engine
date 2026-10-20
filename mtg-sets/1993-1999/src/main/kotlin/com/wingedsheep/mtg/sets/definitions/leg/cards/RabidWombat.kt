package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rabid Wombat — Legends #198
 * {2}{G}{G} · Creature — Wombat · 0/1
 *
 * Vigilance
 * This creature gets +2/+2 for each Aura attached to it.
 *
 * The Champion of the Flame shape restricted to Auras: [DynamicAmounts.aurasAttachedToSelf]
 * counts only Auras (Equipment doesn't feed it), recomputed continuously so the bonus changes
 * the moment an Aura is attached or removed.
 */
val RabidWombat = card("Rabid Wombat") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wombat"
    power = 0
    toughness = 1
    oracleText = "Vigilance\nThis creature gets +2/+2 for each Aura attached to it."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.aurasAttachedToSelf() * 2,
            toughnessBonus = DynamicAmounts.aurasAttachedToSelf() * 2
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d9b9eb8-6367-4ab5-8e00-a9c9e1d69032.jpg?1783948045"
        ruling("2005-08-01", "If an Aura is removed from the Wombat, its power and toughness change immediately.")
    }
}
