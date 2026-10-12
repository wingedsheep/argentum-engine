package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hoarding Recluse
 * {3}{G}
 * Creature — Spider
 * 2/3
 * Reach, deathtouch
 * When this creature dies, put up to one other target card from a graveyard on the bottom of its
 * owner's library.
 *
 * "Other" matters because this creature is itself in a graveyard when the trigger targets;
 * `.other()` keeps it from choosing itself. "Up to one" is an optional single target.
 */
val HoardingRecluse = card("Hoarding Recluse") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 3
    oracleText = "Reach, deathtouch\n" +
        "When this creature dies, put up to one other target card from a graveyard on the bottom " +
        "of its owner's library."

    keywords(Keyword.REACH, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(TargetFilter.CardInGraveyard.other(), optional = true)
        effect = Effects.Move(
            target = card,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom,
        )
        description = "When this creature dies, put up to one other target card from a graveyard " +
            "on the bottom of its owner's library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Jorge Jacinto"
        flavorText = "To the spiders of Argoth, thopters were just bigger, crunchier flies."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6559afec-6bb8-4501-8805-4eb108443048.jpg?1783920043"
    }
}
