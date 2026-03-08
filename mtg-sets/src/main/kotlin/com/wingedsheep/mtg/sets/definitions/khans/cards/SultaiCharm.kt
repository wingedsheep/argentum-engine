package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Sultai Charm
 * {B}{G}{U}
 * Instant
 * Choose one —
 * • Destroy target monocolored creature.
 * • Destroy target artifact or enchantment.
 * • Draw two cards, then discard a card.
 */
val SultaiCharm = card("Sultai Charm") {
    manaCost = "{B}{G}{U}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Destroy target monocolored creature.\n• Destroy target artifact or enchantment.\n• Draw two cards, then discard a card."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target monocolored creature") {
                val t = target("target", TargetCreature(
                    filter = TargetFilter(
                        GameObjectFilter(cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsMonocolored))
                    )
                ))
                effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
            }
            mode("Destroy target artifact or enchantment") {
                val t = target("target", TargetPermanent(
                    filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment)
                ))
                effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
            }
            mode("Draw two cards, then discard a card") {
                effect = EffectPatterns.loot(draw = 2, discard = 1)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Mathias Kollros"
        flavorText = "\"Strike,\" the fumes hiss. \"Raise an empire with your ambition.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/993c9028-9b1b-4903-81b2-3cf4f37b7229.jpg?1562790829"
        ruling("2014-09-20", "A monocolored creature is exactly one color. Colorless creatures aren't monocolored.")
    }
}
