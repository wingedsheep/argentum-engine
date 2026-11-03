package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merfolk Branchwalker
 * {1}{G}
 * Creature — Merfolk Scout
 * 2/1
 * When this creature enters, it explores.
 */
val MerfolkBranchwalker = card("Merfolk Branchwalker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Scout"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c6f0d4a-4827-47b6-8af6-803949c195f8.jpg?1783935724"
    }
}
