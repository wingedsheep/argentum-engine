package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You Find the Villains' Lair
 * {1}{U}{U}
 * Instant
 *
 * Choose one —
 * • Foil Their Scheme — Counter target spell.
 * • Learn Their Secrets — Draw two cards, then discard two cards.
 *
 * A plain choose-one `modal(chooseCount = 1)`: a hard counter over [TargetFilter.SpellOnStack]
 * (the Blue Elemental Blast shape), and an untargeted draw-two-then-discard-two loot.
 */
val YouFindTheVillainsLair = card("You Find the Villains' Lair") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Foil Their Scheme — Counter target spell.\n" +
        "• Learn Their Secrets — Draw two cards, then discard two cards."

    spell {
        modal(chooseCount = 1) {
            mode("Foil Their Scheme — Counter target spell.") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterSpell()
            }
            mode("Learn Their Secrets — Draw two cards, then discard two cards.") {
                effect = Effects.DrawCards(2) then Effects.Discard(2)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6704458-6e9e-4795-a56d-25b68fbf9672.jpg?1783926504"
    }
}
