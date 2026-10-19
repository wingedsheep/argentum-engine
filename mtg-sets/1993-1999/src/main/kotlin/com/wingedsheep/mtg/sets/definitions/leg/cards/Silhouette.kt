package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silhouette
 * {1}{U}
 * Instant
 *
 * Choose target creature. If a spell or ability that targets that creature would cause a source
 * to deal damage to that creature this turn, prevent that damage.
 *
 * The cause, not the source, is what's judged: [PreventionSourceFilter.TargetingRecipient] covers
 * damage while the resolving spell or ability has the creature among its targets. Combat damage
 * and untargeted sweepers still get through.
 */
val Silhouette = card("Silhouette") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose target creature. If a spell or ability that targets that creature would cause a " +
        "source to deal damage to that creature this turn, prevent that damage."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.PreventDamage(target = creature, sources = PreventionSourceFilter.TargetingRecipient)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6d6fac6-9a23-465f-a813-92e1ed1cd742.jpg?1783948071"
    }
}
