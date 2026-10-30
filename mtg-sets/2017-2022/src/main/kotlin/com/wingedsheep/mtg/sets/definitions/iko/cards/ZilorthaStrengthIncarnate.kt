package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LethalDamageDeterminedByPower
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Zilortha, Strength Incarnate
 * {3}{R}{G}
 * Legendary Creature — Dinosaur
 * 7/3
 * Trample
 * Lethal damage dealt to creatures you control is determined by their power rather than their toughness.
 *
 * Its only IKO printing is the Godzilla Series card (#275, "Godzilla, King of the Monsters").
 */
val ZilorthaStrengthIncarnate = card("Zilortha, Strength Incarnate") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Dinosaur"
    power = 7
    toughness = 3
    oracleText = "Trample\n" +
        "Lethal damage dealt to creatures you control is determined by their power rather than their toughness."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = LethalDamageDeterminedByPower(GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "275"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a0639a0-c898-4a07-975c-a02bdd53175b.jpg?1783930992"
        ruling("2020-04-17", "Any time the game is checking whether damage is lethal or if a creature should be destroyed for having lethal damage marked on it, use the power of your creatures rather than their toughness to check the damage against. This includes being assigned trample damage, damage from Flame Spill, and so on.")
        ruling("2020-04-17", "A creature with 0 power isn't destroyed unless it has at least 1 damage marked on it.")
        ruling("2020-04-17", "A creature with 0 toughness is put into its owner's graveyard. This state-based action isn't a consequence of damage, so Zilortha doesn't affect it.")
        ruling("2020-04-17", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a creature you control may become lethal if Zilortha enters or leaves the battlefield during that turn.")
    }
}
