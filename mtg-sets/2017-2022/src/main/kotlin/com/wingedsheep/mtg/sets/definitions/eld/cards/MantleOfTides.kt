package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mantle of Tides
 * {U}
 * Artifact — Equipment
 * Equipped creature gets +1/+2.
 * Whenever you draw your second card each turn, attach this Equipment to target creature you control.
 * Equip {3}
 *
 * Faerie Vandal's `drawsNth(2)` trigger driving Rosethorn Halberd's targeted attach. The attach is
 * mandatory (no "may"); an illegal target on resolution fizzles the ability and leaves the
 * Equipment where it was.
 */
val MantleOfTides = card("Mantle of Tides") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+2.\n" +
        "Whenever you draw your second card each turn, attach this Equipment to target creature you control.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+1, +2, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(creature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c058d01e-f705-4407-bd9e-a2d127afdf04.jpg?1783932656"
        ruling("2019-10-04", "The triggered ability of Mantle of Tides can target the creature it's already attached to.")
        ruling(
            "2019-10-04",
            "If the target creature is an illegal target by the time Mantle of Tides's triggered ability tries to " +
                "resolve, the ability won't resolve. Mantle of Tides remains attached to what it was attached to or " +
                "remains unattached if it wasn't attached."
        )
        ruling(
            "2019-10-04",
            "The triggered ability can trigger only once each turn. It doesn't matter whether the permanent with " +
                "that ability was on the battlefield when the first card was drawn. If it's not on the battlefield " +
                "when the second card is drawn, the ability can't trigger at all that turn. It won't trigger when " +
                "the third or fourth card is drawn."
        )
    }
}
