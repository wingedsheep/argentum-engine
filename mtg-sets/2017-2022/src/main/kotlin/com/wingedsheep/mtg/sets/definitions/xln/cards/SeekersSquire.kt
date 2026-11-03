package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seekers' Squire
 * {1}{B}
 * Creature — Human Scout
 * 1/2
 * When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)
 */
val SeekersSquire = card("Seekers' Squire") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Scout"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Anthony Palumbo"
        flavorText = "Dusk follows the light."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e37a6802-7a1d-4d37-bd65-f3e3e5127aca.jpg?1783935755"
    }
}
