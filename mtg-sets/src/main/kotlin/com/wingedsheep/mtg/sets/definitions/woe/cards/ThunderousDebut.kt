package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thunderous Debut
 * {6}{G}{G}
 * Sorcery
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Look at the top twenty cards of your library. You may reveal up to two creature cards from
 * among them. If this spell was bargained, put the revealed cards onto the battlefield.
 * Otherwise, put the revealed cards into your hand. Then shuffle.
 *
 * The spell-rider shape of bargain (CR 702.166c) — but here the payoff isn't a bigger number
 * ([FarsightRitual]) or an extra clause ([ArchonsGlory]); it's a different *destination* for the
 * same picked cards. So the branch is a [ConditionalEffect] on [Conditions.WasBargained] over two
 * moves of the one collection rather than two separate digs, which keeps the look-and-pick
 * identical either way and lets the player choose before knowing nothing new.
 *
 * The dig is a [Effects.Pipeline]: gather twenty off the top unrevealed (the card says "look at"),
 * `chooseUpTo(2)` narrowed to creature cards with `showAllCards` so the player still sees the
 * whole twenty, then an explicit reveal of the picks — "you may reveal" is optional and filtered,
 * which is exactly `ChooseUpTo` over a filter, and picking zero is legal.
 *
 * Nothing puts the other eighteen back: a gather off the top of the library only *reads* the ids,
 * so anything not moved is still sitting in the library when [ShuffleLibraryEffect] runs for
 * "Then shuffle". A library shorter than twenty simply yields what's there.
 */
val ThunderousDebut = card("Thunderous Debut") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Look at the top twenty cards of your library. You may reveal up to two creature cards " +
        "from among them. If this spell was bargained, put the revealed cards onto the " +
        "battlefield. Otherwise, put the revealed cards into your hand. Then shuffle."

    bargain()

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(20)))
            val chosen = chooseUpTo(
                2,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "Reveal up to two creature cards",
                showAllCards = true,
            )
            // The controller hand-picked these, so only the opponents need the reveal overlay.
            reveal(chosen, revealToSelf = false)
            run(
                ConditionalEffect(
                    condition = Conditions.WasBargained,
                    effect = MoveCollectionEffect(
                        from = chosen.key,
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    ),
                    elseEffect = MoveCollectionEffect(
                        from = chosen.key,
                        destination = CardDestination.ToZone(Zone.HAND),
                    ),
                )
            )
            run(ShuffleLibraryEffect())
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d98f51ec-8eae-434a-ab62-85bdb6586fa2.jpg?1783915075"

        ruling(
            "2023-09-01",
            "Bargain represents an optional additional cost. A spell cast with that additional " +
                "cost paid is \"bargained.\""
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "If you copy a bargained spell, the copy is also bargained. If a card or token enters " +
                "the battlefield as a copy of a permanent that's already on the battlefield, the " +
                "new permanent isn't bargained, even if the original was."
        )
    }
}
