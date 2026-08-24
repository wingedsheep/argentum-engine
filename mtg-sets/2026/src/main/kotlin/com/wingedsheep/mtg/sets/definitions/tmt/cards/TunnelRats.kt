package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tunnel Rats
 * {1}{B}
 * Creature — Rat
 * 2/2
 *
 * {4}{B}: Return this card from your graveyard to the battlefield
 * tapped.
 */
val TunnelRats = card("Tunnel Rats") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    oracleText = "{4}{B}: Return this card from your graveyard to the battlefield tapped."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Daniel Romanovsky"
        flavorText = "The Rat King's subjects are everywhere you don't want to be."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70faf7d8-008a-454a-a21b-702aa661b8f9.jpg?1771586928"
    }
}
