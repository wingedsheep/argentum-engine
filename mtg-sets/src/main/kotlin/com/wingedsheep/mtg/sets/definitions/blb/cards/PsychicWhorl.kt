package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.dsl.HandPatterns
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Psychic Whorl
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards. Then if you control a Rat, surveil 2.
 */
val PsychicWhorl = card("Psychic Whorl") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards. Then if you control a Rat, surveil 2."

    spell {
        val t = target("target opponent", TargetOpponent())
        effect = HandPatterns.discardCards(2, t)
            .then(ConditionalEffect(
                condition = Conditions.ControlCreatureOfType(Subtype("Rat")),
                effect = LibraryPatterns.surveil(2)
            ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df900308-8432-4a0a-be21-17482026012b.jpg?1721426473"
    }
}
