package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wall of Wonder
 * {2}{U}{U}
 * Creature — Wall
 * 1/5
 *
 * Defender (This creature can't attack.)
 * {2}{U}{U}: This creature gets +4/-4 until end of turn and can attack this turn as though it
 * didn't have defender.
 */
val WallOfWonder = card("Wall of Wonder") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    power = 1
    toughness = 5
    oracleText = "Defender (This creature can't attack.)\n" +
        "{2}{U}{U}: This creature gets +4/-4 until end of turn and can attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{2}{U}{U}")
        effect = Effects.ModifyStats(4, -4, EffectTarget.Self) then
            Effects.CanAttackDespiteDefenderThisTurn(EffectTarget.Self)
        description = "{2}{U}{U}: This creature gets +4/-4 until end of turn and can attack this turn " +
            "as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Richard Thomas"
        flavorText = "So confusing is the Wall's appearance that few of its victims even see it move."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcd9af40-b46c-44b4-878e-8eb026c96b51.jpg?1783948070"
    }
}
