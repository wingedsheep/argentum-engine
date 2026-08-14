package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

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
    //
    // NOT a printed "Equip [quality]" card. Ghostfire Blade prints one equip ability, "Equip {3}",
    // plus a cost reduction ("costs {2} less to activate if it targets a colorless creature") — no
    // CR 702.6c quality restriction is involved. This {1} ability is a *model* of that reduction:
    // a second equip whose target filter matches exactly the creatures the discount applies to.
    // Behaviour is equivalent (a colorless target can always be equipped for {1}, anything else for
    // {3}), and the two-ability shape is retained only because it predates the facade's
    // quality/targetFilter pair.
    //
    // If it were re-modelled, the mechanism would be `equipAbility`'s existing
    // `genericCostReduction` rail (a `DynamicAmount` evaluated against the chosen target — the same
    // one Dragonfire Blade uses via `DynamicAmounts.targetColorCount()`), expressing it as the
    // single printed ability it is.
    equipAbility(
        "{1}",
        quality = "colorless",
        targetFilter = TargetFilter(
            GameObjectFilter(
                cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsColorless)
            ).youControl()
        ),
    )

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
