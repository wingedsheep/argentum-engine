package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gandalf's Sanction
 * {1}{U}{R}
 * Sorcery
 *
 * Gandalf's Sanction deals X damage to target creature, where X is the number of instant and
 * sorcery cards in your graveyard. Excess damage is dealt to that creature's controller instead.
 *
 * Uses the new `DealDamageExcessToController` (DealDamageEffect.excessToController) — the creature
 * takes only the lethal portion and the excess (CR 120.4a) is dealt to its controller.
 */
val GandalfsSanction = card("Gandalf's Sanction") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Gandalf's Sanction deals X damage to target creature, where X is the number of " +
        "instant and sorcery cards in your graveyard. Excess damage is dealt to that creature's " +
        "controller instead."

    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.DealDamageExcessToController(
            amount = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery).count(),
            target = t,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Tatiana Veryayskaya"
        flavorText = "There was a crack, and the staff split asunder in Saruman's hand, and the head of it fell down at Gandalf's feet."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ebf2a25-9bee-4146-af83-f22aab6db2a8.jpg?1688134626"
    }
}
