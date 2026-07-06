package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * Cenote Scout — {G}
 * Creature — Merfolk Scout
 * 1/1
 * When this creature enters, it explores.
 */
val CenoteScout = card("Cenote Scout") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Scout"
    oracleText = "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Caroline Gariba"
        flavorText = "\"If you're afraid to dive into the unknown, how will you ever find anything new?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ce4b7bc-636b-4723-a7c1-2c859f333492.jpg?1782694467"
    }
}
