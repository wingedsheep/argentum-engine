package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Consuming Vapors — Rise of the Eldrazi #101 (canonical printing)
 * {3}{B} · Sorcery
 *
 * Target player sacrifices a creature of their choice. You gain life equal to that creature's
 * toughness.
 * Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your
 * next upkeep, you may cast this card from exile without paying its mana cost.)
 *
 * Tribute to Hunger's edict-then-lifegain shape (the sacrificed creature's last-known toughness) with
 * a *player* target instead of an opponent, plus the bare [Keyword.REBOUND] that Staggershock uses.
 * The rebound cast chooses a fresh target player.
 */
val ConsumingVapors = card("Consuming Vapors") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player sacrifices a creature of their choice. You gain life equal to that creature's toughness.\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        val t = target(Targets.Player)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, t) then
            Effects.GainLife(DynamicAmounts.sacrificedToughness(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Trevor Claxton"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/008de17e-aa79-4d4c-ab66-d516eca49e42.jpg?1783941987"
    }
}
