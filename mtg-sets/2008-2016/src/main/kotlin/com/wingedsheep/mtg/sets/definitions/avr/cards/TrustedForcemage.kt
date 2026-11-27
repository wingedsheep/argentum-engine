package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Trusted Forcemage — Avacyn Restored #199 (canonical printing)
 * {2}{G} · Creature — Human Shaman · 2/2
 *
 * Soulbond
 * As long as this creature is paired with another creature, each of those creatures gets +1/+1.
 *
 * [DruidsFamiliar] at +1/+1: [ModifyStats] over `GroupFilter.soulbondPair()`, empty while unpaired.
 */
val TrustedForcemage = card("Trusted Forcemage") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, each of those creatures gets +1/+1."
    power = 2
    toughness = 2

    soulbond()

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ee66ef9-10a7-4aab-88f7-84956811cc6c.jpg?1783940658"
    }
}
