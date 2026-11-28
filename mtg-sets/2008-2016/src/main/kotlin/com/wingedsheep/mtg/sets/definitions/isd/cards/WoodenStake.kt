package com.wingedsheep.mtg.sets.definitions.isd.cards

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
 * Wooden Stake
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+0.
 * Whenever equipped creature blocks or becomes blocked by a Vampire, destroy that creature.
 * It can't be regenerated.
 * Equip {1}
 *
 * Barrow-Blade's trigger shape: `Triggers.attached.blocksOrBecomesBlocked(by)` fires off the
 * equipped creature's combat once per matching Vampire partner, and the partner is the
 * triggering entity — "that creature". The destroy happens on resolution in the declare
 * blockers step, before combat damage (2011-09-22 ruling).
 */
val WoodenStake = card("Wooden Stake") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0.\n" +
        "Whenever equipped creature blocks or becomes blocked by a Vampire, destroy that creature. " +
        "It can't be regenerated.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.blocksOrBecomesBlocked(GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE))
        effect = Effects.Destroy(EffectTarget.TriggeringEntity, noRegenerate = true)
        description = "Whenever equipped creature blocks or becomes blocked by a Vampire, " +
            "destroy that creature. It can't be regenerated."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "David Palumbo"
        ruling("2011-09-22", "The Vampire is destroyed before any combat damage is dealt.")
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e2825f5-8112-4108-910a-4303b2d57356.jpg?1783940895"
    }
}
