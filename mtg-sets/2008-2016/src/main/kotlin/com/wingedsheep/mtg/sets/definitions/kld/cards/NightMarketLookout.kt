package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Night Market Lookout
 * {B}
 * Creature — Human Rogue
 * 1/1
 * Whenever this creature becomes tapped, each opponent loses 1 life and you gain 1 life.
 */
val NightMarketLookout = card("Night Market Lookout") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    oracleText = "Whenever this creature becomes tapped, each opponent loses 1 life and you gain 1 life."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Nils Hamm"
        flavorText = "The Consulate rarely cracks the coded announcement of a night market's location before dawn, and enforcers are taken care of if they wander too close."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4edb03b7-e5a2-4ba1-b0ec-bfbfeaa94efd.jpg?1783937203"
    }
}
