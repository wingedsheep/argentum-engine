package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * Pathfinding Axejaw — {3}{G}
 * Creature — Dinosaur
 * 4/3
 * When this creature enters, it explores.
 */
val PathfindingAxejaw = card("Pathfinding Axejaw") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    oracleText = "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Raoul Vitale"
        flavorText = "When you don't know what dangers you'll face, bring the biggest danger with you."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f619075-ca5d-4e09-bd84-31e6b61eaa7e.jpg?1782694444"
    }
}
