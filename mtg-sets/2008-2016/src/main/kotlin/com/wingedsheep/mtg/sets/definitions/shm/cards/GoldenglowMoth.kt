package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Goldenglow Moth
 * {W}
 * Creature — Insect
 * 0 / 1
 *
 * Flying
 * Whenever this creature blocks, you may gain 4 life.
 *
 * - [Triggers.Blocks] is the SELF-bound "this creature blocks" event, so it fires once the Moth is
 *   declared as a blocker — not on the attacker becoming blocked, and regardless of whether combat
 *   damage is ever dealt.
 * - "You may gain 4 life" is a resolution-time yes/no with no cost, so it is the `optional = true`
 *   shorthand (a [com.wingedsheep.sdk.scripting.effects.Gate.MayDecide] around the gain).
 */
val GoldenglowMoth = card("Goldenglow Moth") {
    manaCost = "{W}"
    typeLine = "Creature — Insect"
    power = 0
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever this creature blocks, you may gain 4 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Blocks
        optional = true
        effect = Effects.GainLife(4)
        description = "Whenever this creature blocks, you may gain 4 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Howard Lyon"
        flavorText = "Ordinary moths follow it, drawn to its light."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12030927-7af2-448f-bc6b-c2035e0b799d.jpg?1783942769"
    }
}
