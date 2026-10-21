package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brazen Dwarf
 * {1}{R}
 * Creature — Dwarf Shaman
 * 1/3
 * Whenever you roll one or more dice, this creature deals 1 damage to each opponent.
 */
val BrazenDwarf = card("Brazen Dwarf") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Shaman"
    oracleText = "Whenever you roll one or more dice, this creature deals 1 damage to each opponent."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.rollsDice()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Gabor Szikszai"
        flavorText = "\"My grandmother saved the life of a brass dragon, and in return she was given a glimpse of the fire in its heart—a glimpse she passed down to me.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3658e65b-df54-4327-9104-c2f3dd65df85.jpg?1783926483"
    }
}
