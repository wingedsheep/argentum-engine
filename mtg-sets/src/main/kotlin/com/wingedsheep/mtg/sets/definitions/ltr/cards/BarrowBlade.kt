package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Barrow-Blade
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature gets +1/+1.
 * Whenever equipped creature blocks or becomes blocked by a creature, that creature loses
 * all abilities until end of turn.
 * Equip {1}
 *
 * The blocks-or-blocked trigger uses the existing `BlocksOrBecomesBlockedBy` event with the
 * new `ATTACHED` binding (so it fires off the equipped creature's combat), and the partner
 * (TriggeringEntity) loses all abilities via the existing `RemoveAllAbilities` effect.
 */
val BarrowBlade = card("Barrow-Blade") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature blocks or becomes blocked by a creature, that creature " +
        "loses all abilities until end of turn.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.BlocksOrBecomesBlockedBy(
            filter = GameObjectFilter.Creature,
            binding = TriggerBinding.ATTACHED
        )
        effect = Effects.RemoveAllAbilities(EffectTarget.TriggeringEntity, Duration.EndOfTurn)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0eb7284-78a5-4b5e-8f6d-6be540e0bef8.jpg?1686970135"
    }
}
