package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Wolfir Silverheart — Avacyn Restored #206 (canonical printing)
 * {3}{G}{G} · Creature — Wolf Warrior · 4/4
 *
 * Soulbond
 * As long as this creature is paired with another creature, each of those creatures gets +4/+4.
 *
 * [TrustedForcemage] at +4/+4: [ModifyStats] over `GroupFilter.soulbondPair()`, empty while unpaired.
 */
val WolfirSilverheart = card("Wolfir Silverheart") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf Warrior"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, each of those creatures gets +4/+4."
    power = 4
    toughness = 4

    soulbond()

    staticAbility {
        ability = ModifyStats(4, 4, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8629c598-11c8-4911-acfb-0643e5feffa8.jpg?1783940656"
    }
}
