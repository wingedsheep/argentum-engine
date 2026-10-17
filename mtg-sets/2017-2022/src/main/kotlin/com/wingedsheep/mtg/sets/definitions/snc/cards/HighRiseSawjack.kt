package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * High-Rise Sawjack
 * {2}{G}
 * Creature — Elf Citizen
 * 2 / 3
 * Reach
 * Whenever this creature blocks a creature with flying, this creature gets +2/+0 until end of turn.
 */
val HighRiseSawjack = card("High-Rise Sawjack") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Citizen"
    oracleText = "Reach (This creature can block creatures with flying.)\nWhenever this creature blocks a creature with flying, this creature gets +2/+0 until end of turn."
    power = 2
    toughness = 3

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Randy Vargas"
        flavorText = "Slicing girders, rescuing trapped workers, killing gigantic rust flies—one tool does it all!"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2399c6d7-57f9-4100-ad64-3c8897a438f7.jpg?1783923102"
    }
}
