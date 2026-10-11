package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Goaded
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Martial Impetus
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+1 and is goaded.
 * Whenever enchanted creature attacks, each other creature that's attacking one of your opponents
 * gets +1/+1 until end of turn.
 *
 * [Goaded] scoped to the attached creature makes the Aura's controller its goader for as long as
 * the Aura stays on it — no "until your next turn" expiry. "Other" is relative to the enchanted
 * creature, the trigger's triggering entity; "your opponents" is the Aura controller's.
 */
val MartialImpetus = card("Martial Impetus") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and is goaded. (It attacks each combat if able and attacks a " +
        "player other than you if able.)\n" +
        "Whenever enchanted creature attacks, each other creature that's attacking one of your " +
        "opponents gets +1/+1 until end of turn."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = Goaded()
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.attackingAnOpponent()).otherThanTriggeringEntity(),
            effect = Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25656d3d-dc5a-4085-8461-cab11b96e70a.jpg?1783931225"
    }
}
