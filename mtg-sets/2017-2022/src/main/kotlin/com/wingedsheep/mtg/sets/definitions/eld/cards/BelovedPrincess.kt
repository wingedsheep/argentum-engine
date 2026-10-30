package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Beloved Princess
 * {W}
 * Creature — Human Noble
 * 1/1
 *
 * Lifelink
 * This creature can't be blocked by creatures with power 3 or greater.
 */
val BelovedPrincess = card("Beloved Princess") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Noble"
    oracleText = "Lifelink\n" +
        "This creature can't be blocked by creatures with power 3 or greater."
    power = 1
    toughness = 1

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = CantBeBlockedBy(blockerFilter = GameObjectFilter.Creature.powerAtLeast(3))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Howard Lyon"
        flavorText = "Sometimes a gentle slipper can travel where an armored boot's been denied."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c6ee4eb-70b2-4f99-9d54-5caf2d3713be.jpg?1783932677"
    }
}
