package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ixalli's Diviner
 * {1}{G}
 * Creature — Human Druid
 * 0/3
 * When this creature enters, it explores.
 */
val IxallisDiviner = card("Ixalli's Diviner") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 0
    toughness = 3
    oracleText = "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "192"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b22b55f6-681f-45da-bc57-a0cf7b8f671d.jpg?1783935725"
    }
}
