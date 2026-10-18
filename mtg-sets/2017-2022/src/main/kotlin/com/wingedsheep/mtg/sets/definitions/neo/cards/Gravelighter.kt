package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gravelighter — Kamigawa: Neon Dynasty #98 (canonical printing)
 * {2}{B} · Creature — Spirit · 2/2
 *
 * Flying
 * When this creature enters, draw a card if a creature died this turn. Otherwise, each player
 * sacrifices a creature of their choice.
 *
 * "If a creature died this turn" is not an intervening-if: the trigger always goes on the stack and
 * the condition is checked on resolution, choosing between the draw and the symmetric edict.
 */
val Gravelighter = card("Gravelighter") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\nWhen this creature enters, draw a card if a creature died this turn. " +
        "Otherwise, each player sacrifices a creature of their choice."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.CreatureDiedThisTurn,
            then = Effects.DrawCards(1),
            otherwise = Effects.ForEachPlayer(
                players = Player.Each,
                effect = Effects.Sacrifice(
                    filter = GameObjectFilter.Creature,
                    count = 1,
                    target = EffectTarget.Controller,
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Miranda Meeks"
        flavorText = "It appears only to those whose lives are ending, welcoming them to their fate."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/532c5b14-75cf-4e4c-9b80-2d56a86e2844.jpg?1783923886"
    }
}
