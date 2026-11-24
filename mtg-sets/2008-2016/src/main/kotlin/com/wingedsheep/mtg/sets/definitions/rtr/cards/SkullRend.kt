package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skull Rend — Return to Ravnica #195 (canonical printing)
 * {3}{B}{R} · Sorcery
 *
 * Skull Rend deals 2 damage to each opponent. Those players each discard two cards at random.
 *
 * The discard iterates each opponent so every player loses two cards from their own hand
 * (inside [Effects.ForEachPlayer] the controller-relative discard binds to the iterated player).
 */
val SkullRend = card("Skull Rend") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Skull Rend deals 2 damage to each opponent. Those players each discard two cards at random."

    spell {
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.ForEachPlayer(Player.EachOpponent, Patterns.Hand.discardRandom(2, EffectTarget.Controller))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Greg Staples"
        flavorText = "\"We could never accept anyone with a face that pretty or a mind that sane.\"\n—Nyoser, Gore-House ringleader"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c8efb23-bac0-41d2-b4ee-27a6b1fe3134.jpg?1783940332"
    }
}
