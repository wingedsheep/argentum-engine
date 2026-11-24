package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Assassin's Strike — Return to Ravnica #57 (canonical printing)
 * {4}{B}{B} · Sorcery
 *
 * Destroy target creature. Its controller discards a card.
 *
 * "Its controller" is the destroyed creature's last-known controller (CR 608.2h), and that player
 * picks the card from their own hand — not the caster.
 */
val AssassinsStrike = card("Assassin's Strike") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. Its controller discards a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Destroy(t) then Effects.Discard(1, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Chase Stone"
        flavorText = "When Selesnya missionaries moved into the Shanav Quarter, they faced scorn, ridicule, and ultimately martyrdom."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f796e320-9898-45d4-9d7a-6d35de53c9ab.jpg?1783940365"
    }
}
