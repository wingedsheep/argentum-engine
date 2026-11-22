package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Into the Wilds
 * {3}{G}
 * Enchantment
 * At the beginning of your upkeep, look at the top card of your library. If it's a land card,
 * you may put it onto the battlefield.
 *
 * Same top-card pipeline as Explorer's Scope / Raiders' Karve, but the land enters untapped:
 * a non-land (or a declined land) stays on top of the library.
 */
val IntoTheWilds = card("Into the Wilds") {
    manaCost = "{3}{G}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, look at the top card of your library. If it's a land card, you may put it onto the battlefield."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val land = chooseUpTo(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                prompt = "You may put the land onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave on top",
                showAllCards = true
            )
            move(land, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Véronique Meignaud"
        flavorText = "\"Wonders hide where the trees grow thickest.\"\n—Mul Daya proverb"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecfa6c8d-b5b5-4b68-9ad4-c9d8169659d6.jpg?1783939903"
        ruling("2013-07-01", "Putting a land onto the battlefield this way doesn't count as playing a land. You'll still be able to play a land during one of your main phases.")
        ruling("2013-07-01", "If the card is not a land card, or you choose not to put it onto the battlefield, it will stay on top of your library. In most cases, you'll then draw it during your draw step.")
    }
}
