package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adventuring Gear
 * {1}
 * Artifact — Equipment
 *
 * Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn.
 * Equip {1}
 *
 * The landfall trigger is a one-shot [Effects.ModifyStats] on the [EffectTarget.EquippedCreature]
 * (default duration: until end of turn). It only does anything while the Equipment is attached —
 * with no equipped creature the effect has no target and no-ops.
 */
val AdventuringGear = card("Adventuring Gear") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    // Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn.
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.ModifyStats(2, 2, EffectTarget.EquippedCreature)
        description = "Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Howard Lyon"
        flavorText = "An explorer's essentials in a wild world."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3aa395f2-656e-4bf3-bd9b-6240bd3e2774.jpg?1782715612"
    }
}
