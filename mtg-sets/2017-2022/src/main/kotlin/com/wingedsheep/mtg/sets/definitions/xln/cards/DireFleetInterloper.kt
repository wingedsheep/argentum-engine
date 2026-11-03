package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dire Fleet Interloper
 * {3}{B}
 * Creature — Human Pirate
 * 2/2
 * Menace
 * When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)
 */
val DireFleetInterloper = card("Dire Fleet Interloper") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Menace\nWhen this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "John Severin Brassell"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2e3f7ee-98d0-45bd-97ae-3dc8050085a1.jpg?1783935762"
    }
}
