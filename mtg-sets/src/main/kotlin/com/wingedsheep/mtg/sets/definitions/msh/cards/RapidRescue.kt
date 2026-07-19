package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rapid Rescue
 * {G}
 * Instant — Common (MSH #181)
 *
 * "Mill two cards. You may put a permanent card from among the milled cards into your hand.
 *  You gain 2 life."
 *
 * Implementation:
 *  - The mill is the ordinary Gather (`isMill = true`, so mill events/triggers fire) → move-to-
 *    graveyard pair, kept inline so the same `milled` collection is still addressable for the
 *    second sentence — "from among the **milled** cards" is scoped to those two cards, not to the
 *    whole graveyard, so `Patterns.Library.mill` + a fresh graveyard gather would be wrong.
 *  - "You may put ... a permanent card" is a `chooseUpTo(1)` filtered to permanents: declining is
 *    choosing zero, and a mill that turned up no permanent auto-skips the prompt. No target is
 *    printed, so this is a resolution-time choice, not a `TargetRequirement`.
 *  - The life gain happens regardless of what was milled or kept (CR 608.2 — instructions resolve
 *    in printed order), so it is the unconditional tail of the pipeline.
 */
val RapidRescue = card("Rapid Rescue") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill two cards. You may put a permanent card from among the milled cards into your hand. " +
        "You gain 2 life. (To mill two cards, put the top two cards of your library into your graveyard.)"

    spell {
        effect = Effects.Pipeline {
            // "Mill two cards."
            val milled = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(2), Player.You, isMill = true),
                name = "milled"
            )
            toGraveyard(milled)
            // "You may put a permanent card from among the milled cards into your hand."
            val kept = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card from among the milled cards into your hand",
                selectedLabel = "Put into your hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(kept)
            // "You gain 2 life."
            run(Effects.GainLife(2))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Raymond Bonilla"
        flavorText = "Another day, another disaster."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d739e636-681e-4423-84e3-91efd02b2c94.jpg?1783902914"
    }
}
