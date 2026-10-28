package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TappedForManaType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Forsaken Monument — Zendikar Rising #244
 * {5} · Legendary Artifact
 *
 * Colorless creatures you control get +2/+2.
 * Whenever you tap a permanent for {C}, add an additional {C}.
 * Whenever you cast a colorless spell, you gain 2 life.
 *
 * - The mana ability is a triggered mana ability (off-stack), Ultima's shape widened from lands to
 *   any permanent you control: [AdditionalManaOnSourceTap] gated on [TappedForManaType.COLORLESS]
 *   mirrors the produced {C}. The engine only runs tap bonuses for mana abilities with {T} in their
 *   cost, and fires once per tap however much {C} was produced (both per the 2020-09-25 rulings).
 * - The anthem is not self-excluding: if the Monument is animated it is a colorless creature too.
 */
val ForsakenMonument = card("Forsaken Monument") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Colorless creatures you control get +2/+2.\n" +
        "Whenever you tap a permanent for {C}, add an additional {C}.\n" +
        "Whenever you cast a colorless spell, you gain 2 life."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).youControl(),
            ),
        )
    }

    // Whenever you tap a permanent for {C}, add an additional {C}. (Triggered mana ability.)
    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Permanent.youControl(),
            color = null,
            whenProducing = TappedForManaType.COLORLESS,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter(cardPredicates = listOf(CardPredicate.IsColorless)))
        effect = Effects.GainLife(2)
        description = "Whenever you cast a colorless spell, you gain 2 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "244"
        artist = "Piotr Dura"
        flavorText = "\"We shall forever roam.\"\n—Ayli, high priest of the Eternal Pilgrims"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33568871-dab0-4c09-b9f0-0ae78a46a06d.jpg?1783929316"
        ruling("2020-09-25", "You \"tap a permanent for {C}\" only if you activate a mana ability of that permanent that includes the {T} symbol in its cost, and only if it produces one or more {C} as it resolves.")
        ruling("2020-09-25", "If you tap a permanent for more than one {C}, you add only one additional {C}.")
        ruling("2020-09-25", "Forsaken Monument's last ability won't trigger when you play a land.")
        ruling("2020-09-25", "A land is normally colorless, regardless of what colors of mana that land can produce. If an effect makes a colorless land into a creature without specifying that it becomes a certain color, the land creature gets +2/+2.")
    }
}
