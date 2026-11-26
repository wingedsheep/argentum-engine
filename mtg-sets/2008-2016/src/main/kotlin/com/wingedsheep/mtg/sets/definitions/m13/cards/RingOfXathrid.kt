package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ring of Xathrid
 * {2}
 * Artifact — Equipment
 * {2}: Regenerate equipped creature.
 * At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's black.
 * Equip {1}
 *
 * The regenerate ability is the Equipment's own (no target): it creates a regeneration shield on
 * whatever creature the Ring is attached to as the ability resolves. The upkeep trigger is the
 * Equipment's own, so "your upkeep" is the Equipment controller's. The "if it's black" is not an
 * intervening-if: it is checked only as the ability resolves, against the equipped creature's
 * projected colours (the Ring of Kalonia shape). Unattached, both abilities do nothing.
 */
val RingOfXathrid = card("Ring of Xathrid") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "{2}: Regenerate equipped creature. (The next time that creature would be destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)\n" +
        "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's black.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.Regenerate(EffectTarget.EquippedCreature)
        description = "{2}: Regenerate equipped creature."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.EquippedCreature,
                GameObjectFilter.Creature.withColor(Color.BLACK),
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EquippedCreature),
        )
        description = "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's black."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47e2aa59-63dc-4e28-8cdc-2ca868ff8f59.jpg?1783940458"
    }
}
