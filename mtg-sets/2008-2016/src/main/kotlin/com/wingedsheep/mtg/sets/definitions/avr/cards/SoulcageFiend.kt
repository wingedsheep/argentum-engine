package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soulcage Fiend
 * {1}{B}{B}
 * Creature — Demon
 * 3/2
 * When this creature dies, each player loses 3 life.
 *
 * "Each player" includes the Fiend's controller — [Player.Each], not each opponent.
 */
val SoulcageFiend = card("Soulcage Fiend") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 3
    toughness = 2
    oracleText = "When this creature dies, each player loses 3 life."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature dies, each player loses 3 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Jason A. Engle"
        flavorText = "Vowing to free the souls of her children, Kastinne followed the tormentors into the ghastly network of caves below Stensia."
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dce1b1d3-9602-42bf-b341-d96976ff1e60.jpg?1783940692"
    }
}
