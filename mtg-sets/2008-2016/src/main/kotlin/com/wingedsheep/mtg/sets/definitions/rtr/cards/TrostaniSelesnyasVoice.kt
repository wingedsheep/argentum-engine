package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Trostani, Selesnya's Voice
 * {G}{G}{W}{W}
 * Legendary Creature — Dryad 2/5
 *
 * Whenever another creature you control enters, you gain life equal to that creature's toughness.
 * {1}{G}{W}, {T}: Populate. (Create a token that's a copy of a creature token you control.)
 *
 * The life gain reads the entering creature's toughness on resolution through
 * [DynamicAmounts.triggeringToughness], falling back to last-known information if it has already
 * left the battlefield (as on Righteous Valkyrie). Populate chooses — never targets — a creature
 * token on resolution: [Effects.CreateTokenCopyOfChosenPermanent], as on Growing Ranks.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val TrostaniSelesnyasVoice = card("Trostani, Selesnya's Voice") {
    manaCost = "{G}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Dryad"
    power = 2
    toughness = 5
    oracleText = "Whenever another creature you control enters, you gain life equal to that creature's toughness.\n" +
        "{1}{G}{W}, {T}: Populate. (Create a token that's a copy of a creature token you control.)"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(DynamicAmounts.triggeringToughness())
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}{W}"), Costs.Tap)
        effect = Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "206"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d1d9d86-5666-4e59-9766-137657b4e040.jpg?1783940330"
    }
}
