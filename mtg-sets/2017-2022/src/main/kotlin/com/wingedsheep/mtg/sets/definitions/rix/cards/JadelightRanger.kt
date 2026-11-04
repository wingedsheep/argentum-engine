package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jadelight Ranger
 * {1}{G}{G}
 * Creature — Merfolk Scout Ranger
 * 2/1
 * When this creature enters, it explores, then it explores again.
 *
 * Two sequential explores on itself; the second sees the first's +1/+1 counter and library change.
 */
val JadelightRanger = card("Jadelight Ranger") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Scout Ranger"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, it explores, then it explores again. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard. Then repeat this process.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self) then Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fe5c719-f20d-4469-9750-fb689d5f3fc8.jpg?1783935285"
    }
}
