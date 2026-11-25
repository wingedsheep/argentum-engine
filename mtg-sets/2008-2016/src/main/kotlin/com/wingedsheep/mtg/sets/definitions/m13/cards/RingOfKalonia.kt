package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ring of Kalonia
 * {2}
 * Artifact — Equipment
 * Equipped creature has trample.
 * At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's green.
 * Equip {1}
 *
 * The upkeep trigger is the Equipment's own, so "your upkeep" is the Equipment controller's. The
 * "if it's green" is not an intervening-if: it is checked only as the ability resolves, against the
 * equipped creature's projected colours (Blade of the Bloodchief's [Conditions.EntityMatches] on
 * [EffectTarget.EquippedCreature] shape). Unattached, it does nothing.
 */
val RingOfKalonia = card("Ring of Kalonia") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has trample. (It can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's green.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.EquippedCreature,
                GameObjectFilter.Creature.withColor(Color.GREEN),
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EquippedCreature),
        )
        description = "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's green."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2082e04f-f972-424e-a724-7a5975215538.jpg?1783940458"
    }
}
