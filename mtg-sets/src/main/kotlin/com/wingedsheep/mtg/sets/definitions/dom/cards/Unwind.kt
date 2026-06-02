package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Unwind
 * {2}{U}
 * Instant
 * Counter target noncreature spell. Untap up to three lands.
 */
val Unwind = card("Unwind") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target noncreature spell. Untap up to three lands."

    spell {
        target = Targets.NoncreatureSpell
        effect = Effects.CounterSpell()
            .then(Effects.Composite(listOf(
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land),
                    storeAs = "lands"
                ),
                SelectFromCollectionEffect(
                    from = "lands",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(3)),
                    storeSelected = "toUntap"
                ),
                TapUntapCollectionEffect(
                    collectionName = "toUntap",
                    tap = false
                )
            )))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Anna Steinbauer"
        flavorText = "\"A problem is only a problem if you don't have the tools to correct it.\"\n—Jhoira"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97da6607-9131-4f8b-8af3-63439a59b78b.jpg?1562739909"
    }
}
