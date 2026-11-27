package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Séance
 * {2}{W}{W}
 * Enchantment
 * At the beginning of each upkeep, you may exile target creature card from your graveyard. If you
 * do, create a token that's a copy of that card, except it's a Spirit in addition to its other
 * types. Exile it at the beginning of the next end step.
 *
 * Same exile-then-copy shape as Soul Separator: the exiled card keeps its [CardComponent], so the
 * copy reads "that card" through the same target. `exileAtStep = Step.END` registers a delayed
 * trigger for the next end step — not a characteristic of the token, so a populate copy stays.
 */
val Seance = card("Séance") {
    manaCost = "{2}{W}{W}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each upkeep, you may exile target creature card from your " +
        "graveyard. If you do, create a token that's a copy of that card, except it's a Spirit in " +
        "addition to its other types. Exile it at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        optional = true
        val graveyardCreature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Exile(graveyardCreature) then
            Effects.CreateTokenCopyOfTarget(
                target = graveyardCreature,
                addedSubtypes = setOf(Subtype("Spirit")),
                exileAtStep = Step.END
            )
        description = "At the beginning of each upkeep, you may exile target creature card from " +
            "your graveyard. If you do, create a token that's a copy of that card, except it's a " +
            "Spirit in addition to its other types. Exile it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e00ae92c-af6d-4a00-b102-c6d3bcc394b4.jpg?1783940850"
        ruling("2017-03-14", "The token copies exactly what was printed on the original card and nothing else. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-03-14", "The delayed triggered ability that exiles the token isn't a characteristic of the token. If an effect such as populate copies the token, the new copy won't be exiled.")
    }
}
