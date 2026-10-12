package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard

/**
 * Perennial Behemoth
 * {5}
 * Artifact Creature — Beast
 * 2/7
 * You may play lands from your graveyard.
 * Unearth {G}{G}
 */
val PerennialBehemoth = card("Perennial Behemoth") {
    manaCost = "{5}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Beast"
    power = 2
    toughness = 7
    oracleText = "You may play lands from your graveyard.\n" +
        "Unearth {G}{G} ({G}{G}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    unearth("{G}{G}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cb74e91-f7e5-49dd-8aa8-83a679f88a21.jpg?1783920037"
        ruling("2022-10-14", "Perennial Behemoth doesn't change the times when you can play those land cards. You can still play only one land per turn, and only during your main phase when you have priority and the stack is empty.")
        ruling("2022-10-14", "Perennial Behemoth doesn't allow you to activate abilities (such as cycling) of land cards in your graveyard.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it!")
    }
}
