package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cunning Strike
 * {3}{U}{R}
 * Instant
 *
 * Cunning Strike deals 2 damage to target creature and 2 damage to target player or planeswalker.
 * Draw a card.
 */
val CunningStrike = card("Cunning Strike") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Cunning Strike deals 2 damage to target creature and 2 damage to target player or planeswalker.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(2, creature) then
            Effects.DealDamage(2, victim) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Clint Cearley"
        flavorText = "\"The opponent who blocks the path, becomes the path.\"\n—Shu Yun, the Silent Tempest"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4991f81-3190-4d33-bf09-9d5387cbec11.jpg?1783938674"
        ruling("2014-11-24", "You can't cast Cunning Strike unless you target both a creature and a player. If one target (but not both) is illegal as Cunning Strike resolves, it deals damage to the remaining legal target and you draw a card. If both targets are illegal, the spell won't resolve and none of its effects will happen. You won't draw a card in that case.")
    }
}
