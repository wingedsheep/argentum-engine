package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SpellCostReduction
import com.wingedsheep.sdk.scripting.effects.TapTargetCreaturesEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Eddymurk Crab
 * {5}{U}{U}
 * Creature — Elemental Crab
 * 5/5
 *
 * Flash
 * This spell costs {1} less to cast for each instant and sorcery card in your graveyard.
 * This creature enters tapped if it's not your turn.
 * When this creature enters, tap up to two target creatures.
 */
val EddymurkCrab = card("Eddymurk Crab") {
    manaCost = "{5}{U}{U}"
    typeLine = "Creature — Elemental Crab"
    power = 5
    toughness = 5
    oracleText = "Flash\nThis spell costs {1} less to cast for each instant and sorcery card in your graveyard.\nThis creature enters tapped if it's not your turn.\nWhen this creature enters, tap up to two target creatures."

    keywords(Keyword.FLASH)

    // Cost reduction: {1} less per instant/sorcery in graveyard
    staticAbility {
        ability = SpellCostReduction(
            reductionSource = CostReductionSource.CardsInGraveyardMatchingFilter(
                filter = GameObjectFilter.Companion.InstantOrSorcery
            )
        )
    }

    // Enters tapped if it's not your turn (= enters tapped unless it IS your turn)
    replacementEffect(EntersTapped(unlessCondition = Conditions.IsYourTurn))

    // ETB: tap up to two target creatures
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetCreature(
            count = 2,
            optional = true,
            filter = TargetFilter.Creature
        )
        effect = TapTargetCreaturesEffect(maxTargets = 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6d45abe-4962-47d9-a54e-7e623ea8647c.jpg?1721426076"
    }
}
