package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rust Monster
 * {2}{R}
 * Creature — Beast
 * 2/1
 * First strike
 * Sacrifice an artifact: This creature gets +2/+0 until end of turn.
 */
val RustMonster = card("Rust Monster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    oracleText = "First strike\nSacrifice an artifact: This creature gets +2/+0 until end of turn."
    power = 2
    toughness = 1

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Simon Dominic"
        flavorText = "\"They don't seem scary, but then you're suddenly standing there in your underclothes without a weapon. Then everything looks like a threat.\"\n—Jarro, battle master"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a7c6b2c-9ba0-4fc1-9922-0988acf2dfde.jpg?1783926472"
    }
}
