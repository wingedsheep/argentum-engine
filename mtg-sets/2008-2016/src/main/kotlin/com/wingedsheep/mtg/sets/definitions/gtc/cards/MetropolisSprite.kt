package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Metropolis Sprite (GTC #42)
 * {1}{U}  Creature — Faerie Rogue  1/2
 *
 * Flying
 * {U}: This creature gets +1/-1 until end of turn.
 */
val MetropolisSprite = card("Metropolis Sprite") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    oracleText = "Flying\n{U}: This creature gets +1/-1 until end of turn."
    power = 1
    toughness = 2

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Scott Chou"
        flavorText = "\"Well, if that goblin didn't want his tongue pierced, he shouldn't have stuck it out at me.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f349013-0846-4bec-bdf9-47a3706d9989.jpg"
    }
}
