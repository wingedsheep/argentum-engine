package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Zenith Chronicler
 * {2}
 * Artifact Creature — Phyrexian Construct
 * 3/1
 * Whenever a player casts their first multicolored spell each turn, each other player draws a card.
 *
 * The ordinal runs over the caster's multicolored casts only (`castsNth(1, Multicolored)`).
 * "Each other player" is every player except the caster: a `ForEachPlayer(Player.Each)` loop
 * rebinds "you" to the visited player, so `Not(TriggeringPlayerIs(Player.You))` skips the caster —
 * including the Chronicler's own controller when they are the one who cast the spell.
 */
val ZenithChronicler = card("Zenith Chronicler") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Phyrexian Construct"
    oracleText = "Whenever a player casts their first multicolored spell each turn, each other player draws a card."
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.anyPlayer.castsNth(1, GameObjectFilter.Multicolored)
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.If(
                Conditions.Not(Conditions.TriggeringPlayerIs(Player.You)),
                Effects.DrawCards(1)
            )
        )
        description = "Whenever a player casts their first multicolored spell each turn, each other player draws a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Johann Bodin"
        flavorText = "It watches through the never-ending day, motionless save for the unsettling, twitching dance it performs as each sun reaches its apex."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1431fe83-7dc7-4c40-8d66-6525560e4323.jpg?1783917984"
    }
}
