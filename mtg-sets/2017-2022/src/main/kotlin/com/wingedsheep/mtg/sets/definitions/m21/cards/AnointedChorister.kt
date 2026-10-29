package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anointed Chorister
 * {W}
 * Creature — Human Cleric
 * 1/1
 * Lifelink
 * {4}{W}: This creature gets +3/+3 until end of turn.
 */
val AnointedChorister = card("Anointed Chorister") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "{4}{W}: This creature gets +3/+3 until end of turn."

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Bastien L. Deharme"
        flavorText = "\"I sing of benevolence and mercy, of golden wings bringing comfort to the meek " +
            "and downtrodden.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c977c67-b0c0-40b0-b129-28de094aaf40.jpg?1783930747"
    }
}
