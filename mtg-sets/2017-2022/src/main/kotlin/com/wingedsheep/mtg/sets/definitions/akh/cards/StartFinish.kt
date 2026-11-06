package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Start // Finish — Amonkhet #215
 *
 * Start {2}{W} — Instant: Create two 1/1 white Warrior creature tokens with vigilance.
 * Finish {2}{B} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   As an additional cost to cast this spell, sacrifice a creature.
 *   Destroy target creature.
 *
 * Finish owns its sacrifice: only the half being cast is evaluated (CR 709.3), so casting Start
 * asks for nothing extra.
 */
val StartFinish = card("Start // Finish") {
    layout = CardLayout.SPLIT
    colorIdentity = "WB"

    face("Start") {
        manaCost = "{2}{W}"
        typeLine = "Instant"
        oracleText = "Create two 1/1 white Warrior creature tokens with vigilance."

        spell {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Warrior"),
                keywords = setOf(Keyword.VIGILANCE),
                count = 2,
            )
        }
    }

    face("Finish") {
        manaCost = "{2}{B}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "As an additional cost to cast this spell, sacrifice a creature.\n" +
            "Destroy target creature."
        keywords(Keyword.AFTERMATH)
        additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c6f5433-57cc-4cb3-8621-2575fcbff392.jpg?1783936458"
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
