package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Security Rhox
 * {2}{R}{G}
 * Creature — Rhino Warrior
 * 5/4
 *
 * You may pay {R}{G} rather than pay this spell's mana cost. Spend only mana produced by
 * Treasures to cast it this way.
 */
val SecurityRhox = card("Security Rhox") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Rhino Warrior"
    power = 5
    toughness = 4
    oracleText = "You may pay {R}{G} rather than pay this spell's mana cost. " +
        "Spend only mana produced by Treasures to cast it this way."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{R}{G}"),
        manaSources = GameObjectFilter.Artifact.withSubtype("Treasure"),
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Lie Setiawan"
        flavorText = "Unlucky assassins find out why he's called \"Brick Wall\" Balzano."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0050dd40-9a18-41e4-97e6-fce5bf220ccf.jpg?1783923072"
    }
}
