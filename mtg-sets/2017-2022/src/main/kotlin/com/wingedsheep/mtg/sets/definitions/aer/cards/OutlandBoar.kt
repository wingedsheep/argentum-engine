package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Outland Boar
 * {2}{R}{G}
 * Creature — Boar
 * 4/4
 * This creature can't be blocked by creatures with power 2 or less.
 */
val OutlandBoar = card("Outland Boar") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Boar"
    power = 4
    toughness = 4
    oracleText = "This creature can't be blocked by creatures with power 2 or less."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Winona Nelson"
        flavorText = "The vibrant forest of Peema is awash with aether. Its animals are stronger, faster, and more aggressive than their counterparts elsewhere."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/135912b3-978b-4a9b-8758-7b138b190232.jpg"
        ruling("2017-02-09", "Once a creature with power 3 or greater has blocked Outland Boar, changing the power of the blocking creature won't cause Outland Boar to become unblocked.")
    }
}
