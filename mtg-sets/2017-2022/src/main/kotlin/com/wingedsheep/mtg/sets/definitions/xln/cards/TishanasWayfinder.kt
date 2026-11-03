package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tishana's Wayfinder
 * {2}{G}
 * Creature — Merfolk Scout
 * 2/2
 * When this creature enters, it explores.
 */
val TishanasWayfinder = card("Tishana's Wayfinder") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Scout"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "211"
        artist = "Shreya Shetty"
        flavorText = "\"There is always another way.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af52383b-545e-4b64-93f9-317ba6dcf5a8.jpg?1783935717"
    }
}
