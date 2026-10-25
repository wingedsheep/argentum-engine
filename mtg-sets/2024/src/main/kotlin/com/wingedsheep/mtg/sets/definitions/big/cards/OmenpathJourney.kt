package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Omenpath Journey — {3}{G} Enchantment (The Big Score, mythic).
 *
 * "When this enchantment enters, search your library for up to five land cards that have
 *  different names, exile them, then shuffle.
 *  At the beginning of your end step, choose a card at random exiled with this enchantment
 *  and put it onto the battlefield tapped."
 *
 * Implemented as an atomic pipeline:
 *  - ETB: Gather library lands → ChooseUpTo(5) with OnePerCardName (different names) →
 *    MoveCollection to exile linked to this source → shuffle.
 *  - Your end step: Gather from this source's linked exile → Random(1) → MoveCollection
 *    onto the battlefield tapped.
 *
 * Rulings:
 *  - You may search for fewer than five land cards (or none) — "up to five".
 *  - While in your library, a double-faced card has only its front-face name, so you can't
 *    reveal two of the same DFC this way.
 */
val OmenpathJourney = card("Omenpath Journey") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, search your library for up to five land cards " +
        "that have different names, exile them, then shuffle.\n" +
        "At the beginning of your end step, choose a card at random exiled with this enchantment " +
        "and put it onto the battlefield tapped."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Land),
                search = true
            )
            val found = chooseUpTo(
                5,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to five land cards with different names to exile"
            )
            exile(found, linkToSource = true)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            val chosen = chooseRandom(1, from = exiled)
            move(
                chosen,
                CardDestination.ToZone(
                    Zone.BATTLEFIELD,
                    placement = ZonePlacement.Tapped
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "18"
        artist = "Nereida"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c49c9b72-61c0-4e3a-a3a6-994b149398a9.jpg?1739804210"
    }
}
