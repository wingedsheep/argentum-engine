package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Returned Phalanx — Theros #104 (canonical printing)
 * {1}{B}
 * Creature — Zombie Soldier
 * 3/3
 * Defender
 * {1}{U}: This creature can attack this turn as though it didn't have defender.
 */
val ReturnedPhalanx = card("Returned Phalanx") {
    manaCost = "{1}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Soldier"
    power = 3
    toughness = 3
    oracleText = "Defender\n{1}{U}: This creature can attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.CanAttackDespiteDefenderThisTurn(EffectTarget.Self)
        description = "{1}{U}: This creature can attack this turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Seb McKinnon"
        flavorText = "They lived in different nations and fought in different eras, but as the Returned, they link arms as one."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93613e23-b363-4bcb-8f63-8f1d25f7f174.jpg?1783939771"
    }
}
