package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You're Ambushed on the Road
 * {W}
 * Instant
 *
 * Choose one —
 * • Make a Retreat — Return target creature you control to its owner's hand.
 * • Stand and Fight — Target creature gets +1/+3 until end of turn.
 *
 * A plain choose-one `modal(chooseCount = 1)` with one target per mode: a bounce on a creature
 * you control, and an until-end-of-turn +1/+3 on any creature.
 */
val YoureAmbushedOnTheRoad = card("You're Ambushed on the Road") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Make a Retreat — Return target creature you control to its owner's hand.\n" +
        "• Stand and Fight — Target creature gets +1/+3 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Make a Retreat — Return target creature you control to its owner's hand.") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.ReturnToHand(creature)
            }
            mode("Stand and Fight — Target creature gets +1/+3 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 3, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Eric Deschamps"
        flavorText = "\"Hand over your loot and no one gets hurt!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/726da84a-fdd4-46df-8ba4-f94d582bcce4.jpg?1783926521"
    }
}
