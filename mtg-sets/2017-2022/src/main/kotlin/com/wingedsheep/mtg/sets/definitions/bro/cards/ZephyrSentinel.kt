package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zephyr Sentinel
 * {1}{U}
 * Creature — Human Soldier
 * 2/1
 * Flash
 * Flying
 * When this creature enters, return up to one other target creature you control to its owner's
 * hand. If it was a Soldier, put a +1/+1 counter on this creature.
 *
 * The Glorious Gale / Sunpearl Kirin shape: the Soldier check runs while the target is still on
 * the battlefield (so it reads the projected subtypes — a changeling counts), then the creature is
 * returned. Both happen in one resolution, so the result is the same as bouncing first and
 * consulting last-known information. Choosing no target means no bounce and no counter; a target
 * that became illegal fizzles the whole ability, so no counter either.
 */
val ZephyrSentinel = card("Zephyr Sentinel") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Flash\nFlying\nWhen this creature enters, return up to one other target creature you " +
        "control to its owner's hand. If it was a Soldier, put a +1/+1 counter on this creature."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl, optional = true)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.SOLDIER), creature),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        ) then Effects.ReturnToHand(creature)
        description = "When this creature enters, return up to one other target creature you control " +
            "to its owner's hand. If it was a Soldier, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Inkognit"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f935adbb-d2e3-48d0-b38c-41f3b583fecb.jpg?1783920100"
    }
}
