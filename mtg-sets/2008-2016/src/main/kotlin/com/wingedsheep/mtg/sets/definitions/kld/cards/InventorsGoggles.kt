package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Inventor's Goggles — Kaladesh #218
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/+2.
 * Whenever an Artificer you control enters, you may attach this Equipment to it.
 * Equip {2}
 */
val InventorsGoggles = card("Inventor's Goggles") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+2.\n" +
        "Whenever an Artificer you control enters, you may attach this Equipment to it.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 2, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withSubtype(Subtype.ARTIFICER)).enters()
        optional = true
        effect = Effects.AttachEquipment(EffectTarget.TriggeringEntity)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "218"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00a26fdf-fdce-4939-8c6a-c9dff623072f.jpg?1783937154"
        ruling(
            "2016-09-20",
            "If the Artificer that entered the battlefield leaves the battlefield before Inventor's Goggles " +
                "becomes attached, Inventor's Goggles stays as it was. If it's already attached to another " +
                "creature, it remains attached to that creature."
        )
    }
}
