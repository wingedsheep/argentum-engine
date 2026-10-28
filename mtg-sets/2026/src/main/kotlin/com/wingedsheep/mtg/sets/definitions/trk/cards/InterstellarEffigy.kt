package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Interstellar Effigy
 * {4}{U}
 * Artifact Creature — Construct
 * 4/3
 *
 * When this creature enters, return target creature an opponent controls to its owner's hand.
 */
val InterstellarEffigy = card("Interstellar Effigy") {
    manaCost = "{4}{U}"
    typeLine = "Artifact Creature — Construct"
    colorIdentity = "U"
    oracleText = "When this creature enters, return target creature an opponent controls to its owner's hand."
    power = 4
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ReturnToHand(creature)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Cliff Nielsen"
        flavorText = "\"My alter ego, so to speak. You must admit, he's effective. You would never have been frightened by me.\"\n—Balok"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b7d505f-2a6b-4525-87c7-19e06d49c9df.jpg?1792672545"
    }
}

