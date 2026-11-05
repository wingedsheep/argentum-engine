package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gideon's Defeat
 * {W}
 * Instant
 * Exile target white creature that's attacking or blocking. If it was a Gideon planeswalker, you
 * gain 5 life.
 *
 * "Was" is read before the exile: the gate checks the target while it is still on the battlefield
 * (projected, so an animated Gideon such as Gideon Blackblade on his controller's turn matches as
 * both a creature and a Gideon planeswalker), and both branches exile it.
 */
val GideonsDefeat = card("Gideon's Defeat") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target white creature that's attacking or blocking. If it was a Gideon " +
        "planeswalker, you gain 5 life."

    spell {
        val victim = target(TargetFilter(GameObjectFilter.Creature.attackingOrBlocking().withColor(Color.WHITE)))
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                victim,
                GameObjectFilter.Planeswalker.withSubtype("Gideon")
            ),
            then = Effects.Exile(victim) then Effects.GainLife(5),
            otherwise = Effects.Exile(victim)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Kieran Yanner"
        flavorText = "\"Gideon, how easy it must be to play the protector when you think yourself " +
            "untouchable. How wrong you are.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4722eb81-1aa1-4863-8dfb-d9d6035bd6c6.jpg?1783936062"
    }
}
