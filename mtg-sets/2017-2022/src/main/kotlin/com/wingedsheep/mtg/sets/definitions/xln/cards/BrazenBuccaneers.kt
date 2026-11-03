package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brazen Buccaneers
 * {3}{R}
 * Creature — Human Pirate
 * 2/2
 * Haste
 * When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)
 */
val BrazenBuccaneers = card("Brazen Buccaneers") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhen this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9ca29a73-9606-4422-9187-67e00dcd52aa.jpg?1783935749"
    }
}
