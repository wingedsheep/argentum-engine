package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Shagrat, Loot Bearer
 * {2}{B}{R}
 * Legendary Creature — Orc Soldier
 * 4/4
 *
 * Whenever Shagrat attacks, attach up to one target Equipment to it. Then amass Orcs X, where X is
 * the number of Equipment attached to Shagrat. (Control of the Equipment doesn't change.)
 *
 * Composed: an attacks-trigger whose effect attaches a targeted Equipment to Shagrat (self) and then
 * amasses Orcs by `DynamicAmounts.equipmentAttachedToSelf()` — the Equipment-only attachment count
 * (so Auras/other attachments don't inflate X). The attach is "up to one target" (optional), and the
 * amass count is read *after* the attach resolves, so a freshly-attached Equipment is included.
 */
val ShagratLootBearer = card("Shagrat, Loot Bearer") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Orc Soldier"
    power = 4
    toughness = 4
    oracleText = "Whenever Shagrat attacks, attach up to one target Equipment to it. Then amass Orcs X, " +
        "where X is the number of Equipment attached to Shagrat. (Control of the Equipment doesn't change. " +
        "To amass Orcs X, put X +1/+1 counters on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.Attacks
        // "up to one target Equipment" — any Equipment; control doesn't change, so it's not
        // restricted to Equipment you control.
        val equipment = target(
            "Equipment",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)),
                optional = true
            )
        )
        effect = Effects.Composite(
            Effects.AttachTargetEquipmentToCreature(equipment, EffectTarget.Self),
            Effects.Amass(DynamicAmounts.equipmentAttachedToSelf(), "Orc")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Tatiana Veryayskaya"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0731f64a-15da-4814-82e9-3a42e1657f36.jpg?1686970039"
    }
}
