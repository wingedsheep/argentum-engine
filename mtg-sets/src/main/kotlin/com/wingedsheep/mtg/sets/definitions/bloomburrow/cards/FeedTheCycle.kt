package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker

/**
 * Feed the Cycle
 * {1}{B}
 * Instant
 *
 * As an additional cost to cast this spell, forage or pay {B}.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 * Destroy target creature or planeswalker.
 *
 * Modeled as a modal spell:
 * Mode 1 = pay extra {B} (total {1}{B}{B}) + destroy
 * Mode 2 = forage (total {1}{B} + forage) + destroy
 */
val FeedTheCycle = card("Feed the Cycle") {
    manaCost = "{1}{B}"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, forage or pay {B}. (To forage, exile three cards from your graveyard or sacrifice a Food.)\nDestroy target creature or planeswalker."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: Pay {B} additional — destroy target creature or planeswalker
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                TargetCreatureOrPlaneswalker(),
                "Pay {B} — destroy target creature or planeswalker"
            ),
            // Mode 2: Forage — destroy target creature or planeswalker
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                TargetCreatureOrPlaneswalker(),
                "Forage — destroy target creature or planeswalker"
            )
        )
    }

    // Mode 2 requires forage as additional cost
    additionalCost(AdditionalCost.Forage)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Donato Giancola"
        flavorText = "Sleeping beneath the sumac\n—Squirrelfolk expression meaning \"death\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e017ff8-2936-4a1b-bece-00004cfbad06.jpg?1721426413"
    }
}
