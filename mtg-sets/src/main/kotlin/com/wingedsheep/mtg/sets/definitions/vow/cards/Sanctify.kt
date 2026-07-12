package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Sanctify
 * {1}{W}
 * Sorcery
 * Destroy target artifact or enchantment. You gain 3 life.
 */
val Sanctify = card("Sanctify") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact or enchantment. You gain 3 life."
    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.ArtifactOrEnchantment))
        effect = Effects.Composite(
            Effects.Move(t, Zone.GRAVEYARD, byDestruction = true),
            GainLifeEffect(3)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Kasia 'Kafis' Zielińska"
        flavorText = "\"Olivia Voldaren has faith only in herself. We answer to a higher calling.\"\n—Thalia, Guardian of Thraben"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/880e071a-6d6c-41c4-b2eb-c9e6626d0c7f.jpg?1782703172"
    }
}
