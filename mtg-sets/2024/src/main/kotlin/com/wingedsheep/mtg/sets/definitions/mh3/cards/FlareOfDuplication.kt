package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flare of Duplication {1}{R}{R}
 * Instant
 *
 * You may sacrifice a nontoken red creature rather than pay this spell's mana cost.
 * Copy target instant or sorcery spell. You may choose new targets for the copy.
 *
 * Flare of Denial's alternative-cost shape; the copy is [Effects.CopyTargetSpell], which carries
 * the "you may choose new targets" prompt itself (Uyo, Silent Prophet's shape).
 */
val FlareOfDuplication = card("Flare of Duplication") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "You may sacrifice a nontoken red creature rather than pay this spell's mana cost.\n" +
        "Copy target instant or sorcery spell. You may choose new targets for the copy."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.SacrificePermanent(GameObjectFilter.Creature.withColor(Color.RED).nontoken())
        )
    )

    spell {
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CopyTargetSpell(target = spell)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Olivier Bernard"
        flavorText = "\"Decent. But let me show you what mastery looks like.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/497a10c6-131b-464e-95e4-e47708a24d48.jpg?1783911272"
        ruling(
            "2024-06-07",
            "The controller of a copy can't choose to pay any alternative or additional costs for the copy. " +
                "However, effects based on any alternative or additional costs that were paid for the original " +
                "spell are copied as though those same costs were paid for the copy."
        )
    }
}
