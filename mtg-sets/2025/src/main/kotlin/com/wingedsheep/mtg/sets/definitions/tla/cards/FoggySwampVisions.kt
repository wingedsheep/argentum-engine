package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foggy Swamp Visions
 * {1}{B}{B}
 * Sorcery
 * As an additional cost to cast this spell, waterbend {X}.
 * Exile X target creature cards from graveyards. For each creature card exiled this way, create a
 * token that's a copy of it. At the beginning of your next end step, sacrifice those tokens.
 *
 * X comes from the waterbend {X} cost (`waterbendCost(isX = true)`): the chosen X is both the exact
 * number of targets (`targets(…, exactly = X)`) and the waterbend amount paid by tapping
 * artifacts/creatures. Resolution pipeline: gather the chosen targets, move them to exile (storing
 * the moved ids as "exiled"), then for each exiled card create a token copy of it that sacrifices
 * itself at the next end step (CR — the per-token delayed sacrifice via `sacrificeAtStep`).
 */
val FoggySwampVisions = card("Foggy Swamp Visions") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, waterbend {X}. " +
        "(While paying a waterbend cost, you can tap your artifacts and creatures to help. " +
        "Each one pays for {1}.)\n" +
        "Exile X target creature cards from graveyards. For each creature card exiled this way, " +
        "create a token that's a copy of it. At the beginning of your next end step, sacrifice " +
        "those tokens."

    waterbendCost(isX = true)

    spell {
        targets(TargetFilter.CreatureInGraveyard, exactly = DynamicAmounts.xValue())
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.ChosenTargets)
            val exiledCards = moveTracked(exiled, CardDestination.ToZone(Zone.EXILE))
            run(Effects.ForEachInCollection(
                collection = exiledCards,
                effect = Effects.CreateTokenCopyOfTarget(
                    target = EffectTarget.IterationEntity,
                    sacrificeAtStep = Step.END,
                ),
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Hori Airi"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a46deaa-88f7-4eec-aa99-b85073847918.jpg?1764120708"
    }
}
