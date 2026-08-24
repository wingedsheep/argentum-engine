package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dwarven Soldier
 * {1}{R}
 * Creature — Dwarf Soldier
 * 2/1
 * Whenever this creature blocks or becomes blocked by one or more Orcs, this creature gets +0/+2
 * until end of turn.
 *
 * "By one or more Orcs" is a single trigger however many Orcs are involved, so the trigger takes
 * `oncePerCombat = true`. Without it the detector fans out one trigger per matching partner — the
 * right reading for the singular "blocked by a creature" wording, but two Orc blockers would then
 * pump this twice.
 */
val DwarvenSoldier = card("Dwarven Soldier") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Soldier"
    oracleText = "Whenever this creature blocks or becomes blocked by one or more Orcs, this " +
        "creature gets +0/+2 until end of turn."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.BlocksOrBecomesBlockedBy(
            GameObjectFilter.Creature.withSubtype(Subtype.ORC),
            oncePerCombat = true,
        )
        effect = Effects.ModifyStats(0, 2, EffectTarget.Self)
        description = "Whenever this creature blocks or becomes blocked by one or more Orcs, this creature gets +0/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53a"
        artist = "Rob Alexander"
        flavorText = "\"Let no one say we did not fight until the last . . . .\"\n—Headstone fragment from a mass grave found in the Crimson Peaks"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fe77608-0b33-43f5-83fb-ae993ca1bf7c.jpg?1783947895"
    }
}
