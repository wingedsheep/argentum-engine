package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Woodland Mystic (M20 #344)
 * {1}{G}  Creature — Elf Druid  1/1
 *
 * {T}: Add {G}.
 */
val WoodlandMystic = card("Woodland Mystic") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    oracleText = "{T}: Add {G}."
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "344"
        artist = "Uriah Voth"
        flavorText = "\"I sing to the branches, and they reach to the sky. I sing to the leaves, and they open to the sun. I sing to the roots, and they dig their fingers deep. I sing the song that creation sung.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d6117cf-5cb3-41f3-8756-c01b5e9c760e.jpg"
    }
}
