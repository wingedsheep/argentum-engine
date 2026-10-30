package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sphere of Safety
 * {4}{W}
 * Enchantment
 *
 * Creatures can't attack you or planeswalkers you control unless their controller pays {X} for
 * each of those creatures, where X is the number of enchantments you control.
 *
 * Baird, Steward of Argive's planeswalker-covering [AttackTax] with a live count of the
 * enchantments you control in place of the fixed {1}.
 */
val SphereOfSafety = card("Sphere of Safety") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures can't attack you or planeswalkers you control unless their controller pays {X} for each of those creatures, where X is the number of enchantments you control."

    staticAbility {
        ability = AttackTax(
            amountPerAttacker = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment).count(),
            coversPlaneswalkers = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Slawomir Maniak"
        flavorText = "The Rakdos believe that all are eager to join their revels, even when told otherwise."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce3bd0d8-7e44-4cf2-9012-8ff0bc39417f.jpg?1783940373"
        ruling("2012-10-01", "If you control Sphere of Safety, your opponents can choose not to attack with a creature with an ability that says it attacks if able.")
    }
}
