package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * No One Left Behind
 * {4}{B}
 * Sorcery
 *
 * This spell costs {3} less to cast if it targets a creature card with mana value 3 or less.
 * Return target creature card from your graveyard to the battlefield.
 */
val NoOneLeftBehind = card("No One Left Behind") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell costs {3} less to cast if it targets a creature card with mana value 3 or less.\nReturn target creature card from your graveyard to the battlefield."

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Creature.manaValueAtMost(3),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Chris Cold"
        flavorText = "Dying for the cause is overrated."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a7e030c-98d1-452c-8020-cd92a3ef02c3.jpg?1783920086"
    }
}
