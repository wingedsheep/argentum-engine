package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fallaji Dragon Engine
 * {8}
 * Artifact Creature — Dragon
 * 5/5
 * Prototype {2}{R} — 1/3
 * Flying
 * {2}: This creature gets +1/+0 until end of turn.
 */
val FallajiDragonEngine = card("Fallaji Dragon Engine") {
    manaCost = "{8}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Prototype {2}{R} — 1/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Flying\n" +
        "{2}: This creature gets +1/+0 until end of turn."

    keywordAbility(KeywordAbility.prototype("{2}{R}", 1, 3))
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7761ed0-a784-4dfb-ab6c-0f4b9a411cf3.jpg?1783920061"
    }
}
