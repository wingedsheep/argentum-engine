package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ghostfire Blade
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +2/+2.
 * Equip {3}
 * Ghostfire Blade's equip ability costs {2} less to activate if it targets a colorless creature.
 *
 * Ruling (2014-09-20): Face-down creatures and most artifact creatures are colorless.
 *
 * The reduced equip cost for colorless creatures is modeled as a second equip
 * activated ability with cost {1} that targets colorless creatures only.
 */
val GhostfireBlade = card("Ghostfire Blade") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2.\nEquip {3}\nGhostfire Blade's equip ability costs {2} less to activate if it targets a colorless creature."

    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }

    // Equip {1}: Attach to target colorless creature you control.
    // (Equip costs {2} less for colorless creatures)
    activatedAbility {
        cost = Costs.Mana("{1}")
        timing = TimingRule.SorcerySpeed
        description = "Equip colorless creature {1}"
        val colorlessCreatureYouControl = target(
            "colorless creature you control",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter(
                        cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsColorless)
                    ).youControl()
                )
            )
        )
        effect = Effects.AttachEquipment(colorlessCreatureYouControl)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Cyril Van Der Haegen"
        flavorText = "\"If you fear the dragon's fire, you are unworthy to wield it.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/711145d8-5178-4fdc-8494-4ab680f55b1a.jpg?1562788410"
        ruling("2014-09-20", "Face-down creatures and most artifact creatures are colorless.")
    }
}
