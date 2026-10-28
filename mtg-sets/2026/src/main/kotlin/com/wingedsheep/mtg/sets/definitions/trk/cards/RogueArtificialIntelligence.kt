package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.model.Rarity

/**
 * Rogue Artificial Intelligence
 * {4}{B}{B}
 * Artifact Creature — Construct
 * 6/5
 *
 * When this creature enters, choose one —
 * • You gain 3 life.
 * • Surveil 2. (Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)
 * Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 */
val RogueArtificialIntelligence = card("Rogue Artificial Intelligence") {
    manaCost = "{4}{B}{B}"
    typeLine = "Artifact Creature — Construct"
    colorIdentity = "B"
    oracleText = "When this creature enters, choose one —\n• You gain 3 life.\n• Surveil 2. (Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 6
    toughness = 5
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                Mode.noTarget(Effects.GainLife(3), "You gain 3 life"),
                Mode.noTarget(Effects.Surveil(2), "Surveil 2")
            )
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edcce061-88c9-40bf-97f8-d0d3018e24dd.jpg?1785981516"
    }
}
