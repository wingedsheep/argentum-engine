package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Picklock Prankster // Free the Fae
 * {1}{U}
 * Creature — Faerie Rogue
 * 1/3
 * Flying, vigilance
 *
 * Adventure: Free the Fae — {1}{U}, Instant — Adventure
 * Mill four cards. Then put an instant, sorcery, or Faerie card from among the milled cards
 * into your hand.
 *
 * The Adventure is the standard Gather → Move → Select → Move mill-and-retrieve pipeline (the
 * [CacheGrab][com.wingedsheep.mtg.sets.definitions.blb.cards.CacheGrab] shape). The cards go to the
 * graveyard *first* and the selection reads the `milled` collection afterwards — that ordering is what
 * makes "from among the milled cards" mean the specific four cards rather than "any card in your
 * graveyard", which matters when the graveyard already holds instants.
 *
 * Oracle says "Then put ... into your hand", not "you may put", so this is
 * [SelectionMode.ChooseExactly]`(1)`, not `ChooseUpTo`. The executor auto-selects nothing when the
 * eligible pool is empty (all four milled cards were, say, lands), so the mandatory wording degrades
 * correctly instead of deadlocking. `showAllCards = true` keeps the ineligible milled cards visible
 * so the player can see what the mill actually hit.
 *
 * The three-way "instant, sorcery, or Faerie" is one `Or` predicate rather than three filters: a
 * Faerie *card* is matched by subtype in any zone, so a Faerie creature card and a Faerie instant
 * both qualify.
 */
val PicklockPrankster = card("Picklock Prankster") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    oracleText = "Flying, vigilance"
    power = 1
    toughness = 3

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    adventure("Free the Fae") {
        manaCost = "{1}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Mill four cards. Then put an instant, sorcery, or Faerie card from among the " +
            "milled cards into your hand. (Then exile this card. You may cast the creature later from exile.)"

        spell {
            effect = Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                        storeAs = "milled",
                    ),
                    MoveCollectionEffect(
                        from = "milled",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    ),
                    SelectFromCollectionEffect(
                        from = "milled",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        filter = GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.Or(
                                    listOf(
                                        CardPredicate.IsInstant,
                                        CardPredicate.IsSorcery,
                                        CardPredicate.HasSubtype(Subtype("Faerie")),
                                    ),
                                ),
                            ),
                        ),
                        storeSelected = "selected",
                        showAllCards = true,
                        prompt = "Put an instant, sorcery, or Faerie card into your hand",
                        selectedLabel = "Put in hand",
                        remainderLabel = "Leave in graveyard",
                    ),
                    MoveCollectionEffect(
                        from = "selected",
                        destination = CardDestination.ToZone(Zone.HAND),
                    ),
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Iris Compiet"
        flavorText = "You can't cage mischief."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ebac73a-1ecf-4e6d-87b1-ea560bfeb064.jpg?1783915116"
    }
}
