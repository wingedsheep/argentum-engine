package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sorcerer of the Fang (M20 #114)
 * {1}{B}  Creature — Human Wizard Sorcerer  1/3
 *
 * {5}{B}, {T}: This creature deals 2 damage to target opponent or planeswalker.
 */
val SorcererOfTheFang = card("Sorcerer of the Fang") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard Sorcerer"
    oracleText = "{5}{B}, {T}: This creature deals 2 damage to target opponent or planeswalker."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{B}"), Costs.Tap)
        val t = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Caio Monteiro"
        flavorText = "\"The Great Serpent slumbers now, but all warmbloods will grovel in the dirt at its awakening!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fe9e325-fbe4-4f43-8d68-8525c4d5ce8b.jpg"
    }
}
