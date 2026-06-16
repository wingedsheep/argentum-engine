package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Oracle's Restoration
 * {G}
 * Sorcery
 * Target creature you control gets +1/+1 until end of turn. You draw a card and gain 1 life.
 */
val OraclesRestoration = card("Oracle's Restoration") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +1/+1 until end of turn. You draw a card and gain 1 life."
    spell {
        val t = target("target creature you control", TargetCreature(filter = TargetFilter.Creature.youControl()))
        effect = Effects.ModifyStats(1, 1, t)
            .then(Effects.DrawCards(1))
            .then(Effects.GainLife(1))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Elliot Lang"
        flavorText = "Though the archaic had ravaged Titan's Grave and stolen Jadzi from safety, her compassion soothed its frenzy."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0863a19d-4511-4a78-98dd-d194afd1c39b.jpg"
    }
}
