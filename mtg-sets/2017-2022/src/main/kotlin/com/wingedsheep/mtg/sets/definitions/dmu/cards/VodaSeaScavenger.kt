package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Voda Sea Scavenger
 * {2}{U}
 * Creature — Merfolk Rogue
 * 3/2
 * Domain — When this creature enters, look at the top X cards of your library, where X is the
 * number of basic land types among lands you control. You may put one of those cards on top of
 * your library. Put the rest on the bottom of your library in a random order.
 *
 * Same shape as Thassa's Oracle's look: X is read at resolution, "you may put one" is an up-to-one
 * split, and a domain count of zero gathers nothing.
 */
val VodaSeaScavenger = card("Voda Sea Scavenger") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    power = 3
    toughness = 2
    oracleText = "Domain — When this creature enters, look at the top X cards of your library, where X is the number of basic land types among lands you control. You may put one of those cards on top of your library. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(
                    count = DynamicAmounts.domain(),
                    player = Player.You
                )
            )
            val split = chooseUpToSplit(
                count = 1,
                from = looked,
                prompt = "You may put one card on top of your library",
                selectedLabel = "Put on top of library",
                remainderLabel = "Put on the bottom in a random order"
            )
            toLibraryTop(split.selected)
            toLibraryBottom(split.remainder, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a3cc789-cbb8-4dae-a749-af169832dd7b.jpg?1783921340"
    }
}
