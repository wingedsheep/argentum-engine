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
 * Ring of Thune
 * {2}
 * Artifact — Equipment
 * Equipped creature has vigilance.
 * At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's white.
 * Equip {1}
 *
 * The upkeep trigger is the Equipment's own, so "your upkeep" is the Equipment controller's. The
 * "if it's white" is not an intervening-if: it is checked only as the ability resolves, against the
 * equipped creature's projected colours (Blade of the Bloodchief's [Conditions.EntityMatches] on
 * [EffectTarget.EquippedCreature] shape). Unattached, it does nothing.
 */
val RingOfThune = card("Ring of Thune") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has vigilance. (Attacking doesn't cause it to tap.)\n" +
        "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's white.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.EquippedCreature,
                GameObjectFilter.Creature.withColor(Color.WHITE),
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EquippedCreature),
        )
        description = "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's white."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee2e94f-5b06-4df0-ba87-4499b1ee4dba.jpg?1783940459"
    }
}
