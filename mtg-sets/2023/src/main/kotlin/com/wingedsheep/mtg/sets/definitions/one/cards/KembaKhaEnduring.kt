package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kemba, Kha Enduring — Phyrexia: All Will Be One #19
 * {1}{W} · Legendary Creature — Cat Cleric · Rare
 * 2/2
 *
 * Whenever Kemba or another Cat you control enters, attach up to one target Equipment you control
 * to that creature.
 * Equipped creatures you control get +1/+1.
 * {3}{W}{W}: Create a 2/2 white Cat creature token.
 *
 * Modeling notes:
 *  - "Kemba or another Cat you control" is `Triggers.a(<Cat permanent you control>)` — the
 *    [com.wingedsheep.sdk.scripting.TriggerBinding.ANY] subject includes the source, and Kemba is
 *    herself a Cat (the Oran-Rief Survivalist shape). The bare tribal noun is a *permanent* filter.
 *  - "Up to one target Equipment you control" is an optional target; "that creature" is the
 *    entering permanent, [EffectTarget.TriggeringEntity]. Declining, or the Equipment / creature
 *    being gone on resolution, makes the attach a no-op.
 *  - The lord is a static `ModifyStats` over equipped creatures you control (projected state).
 */
val KembaKhaEnduring = card("Kemba, Kha Enduring") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Cat Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever Kemba or another Cat you control enters, attach up to one target Equipment " +
        "you control to that creature.\n" +
        "Equipped creatures you control get +1/+1.\n" +
        "{3}{W}{W}: Create a 2/2 white Cat creature token."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Cat").youControl()).enters()
        val equipment = target(
            TargetFilter(baseFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
            optional = true,
        )
        effect = Effects.AttachTargetEquipmentToCreature(equipment, EffectTarget.TriggeringEntity)
    }

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.youControl().equipped()))
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}{W}")
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            imageUri = "https://cards.scryfall.io/normal/front/1/1/11011596-4aa1-48a7-90d8-36cb7b27c711.jpg?1783918169",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/945d283d-4592-485f-808a-6e5a721f3cf7.jpg?1783918078"
    }
}
