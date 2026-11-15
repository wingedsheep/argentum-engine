package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Channel Harm
 * {5}{W}
 * Instant
 * Prevent all damage that would be dealt to you and permanents you control this turn by sources you
 * don't control. If damage is prevented this way, you may have Channel Harm deal that much damage
 * to target creature.
 *
 * A recipient-side shield over you and your permanents, narrowed to sources you don't control,
 * whose rest is part of the prevention (no stack): each damage instance it prevents offers the
 * choice to have Channel Harm deal that much damage to the creature targeted as the spell was cast.
 * The target is only checked as Channel Harm resolves; a target that has since left the battlefield
 * is a new object and takes nothing, but the damage is still prevented. Channel Harm is the source
 * of the new damage, so a creature you control that it hits isn't covered by its own shield.
 */
val ChannelHarm = card("Channel Harm") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all damage that would be dealt to you and permanents you control this turn by " +
        "sources you don't control. If damage is prevented this way, you may have Channel Harm deal that " +
        "much damage to target creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.PreventDamage(
            toGroup = GameObjectFilter.Permanent.youControl(),
            alsoToYou = true,
            sources = PreventionSourceFilter.Matching(
                GameObjectFilter.Any.withControllerPredicate(ControllerPredicate.Not(ControllerPredicate.ControlledByYou))
            ),
            onPrevented = Effects.May(
                Effects.DealDamage(DynamicAmounts.preventedDamage(), creature)
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8801465e-295f-4b6b-a321-120f1b1edd0b.jpg?1783938714"
    }
}
