package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackPermanentCostAction
import com.wingedsheep.sdk.scripting.CantAttackUnlessPermanentCost
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Floodtide Serpent
 * {4}{U}
 * Creature — Serpent
 * 4/4
 * This creature can't attack unless you return an enchantment you control to its owner's hand.
 * (This cost is paid as attackers are declared.)
 *
 * Leviathan's attack cost with a bounce in place of the sacrifice: the declaration is illegal
 * without an enchantment to return, and otherwise pauses to ask which one goes back.
 */
val FloodtideSerpent = card("Floodtide Serpent") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 4
    toughness = 4
    oracleText = "This creature can't attack unless you return an enchantment you control to its " +
        "owner's hand. (This cost is paid as attackers are declared.)"

    staticAbility {
        ability = CantAttackUnlessPermanentCost(
            filter = GameObjectFilter.Enchantment,
            action = AttackPermanentCostAction.RETURN_TO_HAND,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Steven Belledin"
        flavorText = "\"Pray to Thassa for *what*? A bigger monster to eat it? A wave that can wash it " +
            "away? What then?\"\n—Lindos, merchant of Meletis"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a2a4b24-096a-4d36-a872-73fa4552bb35.jpg?1783939568"
    }
}
