package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nearheath Pilgrim — Avacyn Restored #31 (canonical printing)
 * {1}{W} · Creature — Human Cleric · 2/1
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have lifelink.
 *
 * Wingcrafter's shape: [GrantKeyword] over `GroupFilter.soulbondPair()`, which is empty while unpaired.
 */
val NearheathPilgrim = card("Nearheath Pilgrim") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have lifelink."
    power = 2
    toughness = 1

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d81d6fe0-c7c2-46a6-811c-f121284937ea.jpg?1783940730"
    }
}
