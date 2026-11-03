package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arterial Flow
 * {1}{B}{B}
 * Sorcery
 *
 * Each opponent discards two cards. If you control a Vampire, each opponent loses 2 life and you
 * gain 2 life.
 */
val ArterialFlow = card("Arterial Flow") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each opponent discards two cards. If you control a Vampire, each opponent loses 2 life and you gain 2 life."

    spell {
        effect = Effects.EachOpponentDiscards(2) then Effects.If(
            Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Vampire")),
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Josh Hass"
        flavorText = "\"It is impious to admit, but I do enjoy pirate blood. Something in the salty tang of it, the spice of rebellion.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13c41212-9f16-48e0-8c4b-985ce331164b.jpg?1783935316"
    }
}
