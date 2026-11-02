package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vicious Rumors
 * {B}
 * Sorcery
 * Vicious Rumors deals 1 damage to each opponent. Each opponent discards a card, then mills a card.
 * You gain 1 life.
 */
val ViciousRumors = card("Vicious Rumors") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Vicious Rumors deals 1 damage to each opponent. Each opponent discards a card, then mills a card. You gain 1 life."

    spell {
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.EachOpponentDiscards(1) then
            Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "David Palumbo"
        flavorText = "\"The alliances were already frayed. All we do is find the loose threads and pluck.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1939952-608e-4786-beeb-a6ff0fa36624.jpg?1783934168"
    }
}
