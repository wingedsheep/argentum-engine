package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Forced Retreat
 * {2}{U}
 * Sorcery
 * Put target creature on top of its owner's library.
 */
val ForcedRetreat = card("Forced Retreat") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Put target creature on top of its owner's library."

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.Move(t, Zone.LIBRARY, ZonePlacement.Top)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Huang Qishi"
        flavorText = "\"Leadership, not numbers, determines victory.\"\n—A Wu commander, before his 5,000 troops forced 15,000 Wei troops to retreat from Ruxu"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9753d211-3436-4a9b-86d9-54a541770ec2.jpg"
    }
}
