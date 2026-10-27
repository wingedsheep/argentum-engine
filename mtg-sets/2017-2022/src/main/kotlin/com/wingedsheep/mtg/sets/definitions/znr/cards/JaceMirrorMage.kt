package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jace, Mirror Mage — Zendikar Rising #63 (canonical printing)
 * {1}{U}{U} · Legendary Planeswalker — Jace · Loyalty 4
 *
 * Kicker {2}
 * When Jace enters, if Jace was kicked, create a token that's a copy of Jace, except it's not
 * legendary and its starting loyalty is 1.
 * +1: Scry 2.
 * 0: Draw a card and reveal it. Remove a number of loyalty counters equal to that card's mana
 *    value from Jace.
 *
 * The kicked copy is an intervening-if ETB (Bubble Snare) minting a token copy of itself whose
 * [CopyExceptions] strip legendary and set the starting loyalty to 1 — both copiable values, so the
 * token enters with one loyalty counter instead of four.
 *
 * The 0 peeks at the card about to be drawn, draws, and keeps it only if it reached the hand
 * (Sindbad), so a replaced draw or an empty library reveals nothing and removes nothing.
 */
val JaceMirrorMage = card("Jace, Mirror Mage") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 4
    oracleText = "Kicker {2} (You may pay an additional {2} as you cast this spell.)\n" +
        "When Jace enters, if Jace was kicked, create a token that's a copy of Jace, except it's not " +
        "legendary and its starting loyalty is 1.\n" +
        "+1: Scry 2.\n" +
        "0: Draw a card and reveal it. Remove a number of loyalty counters equal to that card's mana " +
        "value from Jace."

    keywordAbility(KeywordAbility.kicker("{2}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(
            exceptions = CopyExceptions(
                removedSupertypes = setOf(Supertype.LEGENDARY),
                startingLoyalty = 1,
            ),
        )
    }

    loyaltyAbility(+1) {
        effect = Effects.Scry(2)
    }

    loyaltyAbility(0) {
        effect = Effects.Pipeline {
            val toDraw = gather(CardSource.TopOfLibrary(1))
            run(Effects.DrawCards(1))
            val drawn = filter(toDraw, GameObjectFilter.Any.currentlyIn(Zone.HAND))
            reveal(drawn)
            run(Effects.RemoveCounters(CounterType.LOYALTY, DynamicAmounts.manaValueOf(drawn), EffectTarget.Self))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "63"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe0ed348-e4ed-4b6a-b9d9-03539a6fb42a.jpg?1783929395"
    }
}
