package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Federation Probe
 * {U}
 * Artifact Creature — Construct
 * 1/1
 *
 * Flying
 * When this creature enters, choose one —
 * • Target creature gets +1/+1 until end of turn.
 * • Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val FederationProbe = card("Federation Probe") {
    manaCost = "{U}"
    typeLine = "Artifact Creature — Construct"
    colorIdentity = "U"
    oracleText = "Flying\nWhen this creature enters, choose one —\n• Target creature gets +1/+1 until end of turn.\n• Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gets +1/+1 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 1, creature)
            },
            Mode(effect = Effects.Surveil(1), description = "Surveil 1.")
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Racrufi"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f988ba84-fc78-41a2-b2f2-eebf78cee591.jpg?1785981405"
    }
}

