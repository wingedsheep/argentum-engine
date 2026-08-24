package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Strongarm Thug
 * {2}{B}
 * Creature — Human Mercenary
 * 1 / 1
 *
 * "Target Mercenary card" is the bare tribal noun, so the filter is permanent + subtype (never
 * creature-only); the graveyard is on the [TargetFilter], and "your graveyard" is the owner
 * predicate.
 */
val StrongarmThug = card("Strongarm Thug") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Mercenary"
    oracleText = "When this creature enters, you may return target Mercenary card from your graveyard to your hand."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val t = target(
            "target",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Permanent.withSubtype("Mercenary").ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Rebecca Guay"
        flavorText = "\"No, I insist—let *me* carry all that heavy jewelry.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20aa9108-470c-484d-908a-c31cf6935765.jpg"
    }
}
