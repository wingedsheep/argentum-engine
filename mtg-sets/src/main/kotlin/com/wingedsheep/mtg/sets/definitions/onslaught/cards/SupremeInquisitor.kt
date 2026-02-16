package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SearchTargetLibraryExileEffect

/**
 * Supreme Inquisitor
 * {3}{U}{U}
 * Creature — Human Wizard
 * 1/3
 * Tap five untapped Wizards you control: Search target player's library for up to five cards
 * and exile them. Then that player shuffles.
 */
val SupremeInquisitor = card("Supreme Inquisitor") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Tap five untapped Wizards you control: Search target player's library for up to five cards and exile them. Then that player shuffles."

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Wizard"))
        target = Targets.Player
        effect = SearchTargetLibraryExileEffect(5)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "rk post"
        flavorText = "\"It's hard to fight on an empty mind.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/867de3d2-2178-4931-823e-ff439e1a45ea.jpg?1562927468"
    }
}
