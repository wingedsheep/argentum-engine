package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.SetBasePowerToughnessEffect
/**
 * Atomic Microsizer
 * {U}
 * Artifact — Equipment
 * Equipped creature gets +1/+0.
 * Whenever equipped creature attacks, choose up to one target creature. That creature can't be blocked this turn and has base power and toughness 1/1 until end of turn.
 * Equip {2}
 */
val AtomicMicrosizer = card("Atomic Microsizer") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0.\nWhenever equipped creature attacks, choose up to one target creature. That creature can't be blocked this turn and has base power and toughness 1/1 until end of turn.\nEquip {2}"

    // Static ability: Equipped creature gets +1/+0
    staticAbility {
        ability = ModifyStats(+1, 0, Filters.EquippedCreature)
    }

    // Triggered ability: Whenever equipped creature attacks...
    triggeredAbility {
        trigger = Triggers.attacks(binding = TriggerBinding.ATTACHED)
        val target = target(
            "up to one target creature", 
            com.wingedsheep.sdk.scripting.targets.TargetObject(optional = true, filter = com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
        )
        effect = Effects.Composite(listOf(
            // Can't be blocked this turn
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, target),
            // Has base power and toughness 1/1 until end of turn
            SetBasePowerToughnessEffect(target, 1, 1, com.wingedsheep.sdk.scripting.Duration.EndOfTurn)
        ))
    }

    // Equip ability
    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Gabor Szikszai"
        flavorText = "When packing for a stealth mission, it's important to fold your dimensions tightly."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3554f0c7-ea73-43e9-a061-bbb8ef6abce1.jpg?1752946737"
    }
}
