package com.wingedsheep.mtg.sets.definitions.j25.cards

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
 * Revoke Demise
 * {4}{B}
 * Sorcery
 *
 * This spell costs {2} less to cast if it targets a creature card with mana value 3 or less.
 * Return target creature card from your graveyard to the battlefield. You gain 2 life.
 */
val RevokeDemise = card("Revoke Demise") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if it targets a creature card with mana value 3 or less.\n" +
        "Return target creature card from your graveyard to the battlefield. You gain 2 life."

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.GainLife(2)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Creature.manaValueAtMost(3),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Runa I. Rosenberger"
        flavorText = "\"Arise, sister. Our journey has only just begun.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5503c9aa-faac-427b-9e6e-5272d1b407a7.jpg?1783908868"
        ruling(
            "2024-11-08",
            "If the target creature card is an illegal target as Revoke Demise tries to resolve, " +
                "it won't resolve and none of its effects will happen. You won't gain life.",
        )
    }
}
