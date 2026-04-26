package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flame-Chain Mauler
 * {1}{R}
 * Creature — Elemental Warrior
 * 2/2
 *
 * {1}{R}: This creature gets +1/+0 and gains menace until end of turn.
 */
val FlameChainMauler = card("Flame-Chain Mauler") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Elemental Warrior"
    power = 2
    toughness = 2
    oracleText = "{1}{R}: This creature gets +1/+0 and gains menace until end of turn. " +
        "(It can't be blocked except by two or more creatures.)"

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Kai Carpenter"
        flavorText = "In the fires below Mount Kulrath, artisans shape weapons designed to draw heat and extinguish life."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/752d7e8e-0dd0-4ace-9c89-a8f9ce73775e.jpg?1767732741"
    }
}
