package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Balan, Wandering Knight — Commander 2017 #2 (earliest printing; reprinted in Jumpstart 2022 and
 * Commander Masters).
 * {2}{W}{W} · Legendary Creature — Cat Knight · 3/3
 *
 * First strike
 * Balan has double strike as long as two or more Equipment are attached to it.
 * {1}{W}: Attach all Equipment you control to Balan.
 *
 * Pure composition:
 *  - The double-strike grant is a conditional [GrantKeyword] on the source, gated on
 *    `equipmentAttachedToSelf() >= 2` — the Equipment-only attachment count read off *projected*
 *    subtypes (Loxodon Punisher's amount), so Auras and other attachments don't count.
 *  - The activated ability is Vulshok Battlemaster's sweep narrowed to Equipment *you control*:
 *    [Effects.ForEachInGroup] over your Equipment, each force-attached to Balan via
 *    [Effects.AttachTargetEquipmentToCreature] (which detaches from any current host first, skips an
 *    Equipment that can't legally equip Balan, and does nothing for one already on Balan —
 *    CR 701.3b). The ability has no timing restriction, so it can be activated after first-strike
 *    damage to pick up a second Equipment and deal regular damage too (2017-08-25 ruling).
 */
val BalanWanderingKnight = card("Balan, Wandering Knight") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Cat Knight"
    power = 3
    toughness = 3
    oracleText = "First strike\n" +
        "Balan has double strike as long as two or more Equipment are attached to it.\n" +
        "{1}{W}: Attach all Equipment you control to Balan."

    keywords(Keyword.FIRST_STRIKE)

    // "Balan has double strike as long as two or more Equipment are attached to it."
    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.equipmentAttachedToSelf(),
            ComparisonOperator.GTE,
            2,
        )
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter.source())
    }

    // "{1}{W}: Attach all Equipment you control to Balan."
    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
            effect = Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.IterationEntity,
                creatureTarget = EffectTarget.Self
            )
        )
        description = "{1}{W}: Attach all Equipment you control to Balan."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Svetlin Velinov"
        flavorText = "\"What weapon will you bear against one who's mastered them all?\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d0b279c-9a6d-4a56-878e-0ebf3f609f65.jpg?1783935951"

        ruling("2017-08-25", "Balan's activated ability has no timing restriction. You can activate it any time you have priority.")
        ruling("2017-08-25", "If Balan deals first-strike damage and then gains double strike (most likely because it picked up some Equipment with its activated ability after first-strike damage was dealt), it will also deal regular combat damage.")
    }
}
