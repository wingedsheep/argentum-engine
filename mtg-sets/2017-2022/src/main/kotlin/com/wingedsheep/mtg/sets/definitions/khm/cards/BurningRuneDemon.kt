package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Burning-Rune Demon — Kaldheim #81
 * {4}{B}{B} · Creature — Demon Berserker · 6/6
 *
 * Flying. When this creature enters, you may search your library for exactly two cards not
 * named Burning-Rune Demon that have different names. If you do, reveal those cards. An
 * opponent chooses one of them. Put the chosen card into your hand and the other into your
 * graveyard, then shuffle.
 *
 * The Gifts Ungiven pipeline, gated on "exactly two":
 *  1. `Effects.May` — declining means no search, no reveal, and no shuffle (2021-02-05 ruling).
 *  2. Gather the library (minus Burning-Rune Demon) → controller picks under `OnePerCardName`.
 *     Finding a card is never required (CR 701.23b); a pick of fewer than two is no legal
 *     "exactly two" find, so the `minSize = 2` gate leaves every card in the library — the same
 *     outcome as failing to find.
 *  3. With two found: reveal them, an opponent (`Chooser.Opponent`, the controller picks which
 *     one in multiplayer) chooses one → hand; the other → graveyard.
 *  4. Shuffle — the search happened whether or not anything was found.
 */
val BurningRuneDemon = card("Burning-Rune Demon") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon Berserker"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "When this creature enters, you may search your library for exactly two cards not named " +
        "Burning-Rune Demon that have different names. If you do, reveal those cards. An opponent " +
        "chooses one of them. Put the chosen card into your hand and the other into your graveyard, " +
        "then shuffle."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(
                        Zone.LIBRARY,
                        Player.You,
                        GameObjectFilter.Any.notNamed("Burning-Rune Demon")
                    ),
                    search = true
                )
                val found = chooseUpTo(
                    2,
                    from = searchable,
                    restrictions = listOf(SelectionRestriction.OnePerCardName),
                    prompt = "Search for exactly two cards not named Burning-Rune Demon with different " +
                        "names (choose none to fail to find)"
                )
                ifNotEmpty(found, minSize = 2) {
                    reveal(found)
                    val (toHandCards, toGraveyardCards) = chooseExactlySplit(
                        1,
                        from = found,
                        chooser = Chooser.Opponent,
                        selectedLabel = "Hand",
                        remainderLabel = "Graveyard",
                        prompt = "Choose one of the revealed cards. It goes into its owner's hand; " +
                            "the other goes into their graveyard."
                    )
                    toHand(toHandCards)
                    toGraveyard(toGraveyardCards)
                }
                run(Effects.ShuffleLibrary())
                // CR 701.23 — the search happened whether or not anything was found.
                run(EmitLibrarySearchedEventEffect)
            },
            prompt = "Search your library for two cards with different names?"
        )
        description = "When this creature enters, you may search your library for exactly two cards " +
            "not named Burning-Rune Demon that have different names. If you do, reveal those cards. " +
            "An opponent chooses one of them. Put the chosen card into your hand and the other into " +
            "your graveyard, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "81"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f358a52b-8044-404e-8d04-2ec5903386cc.jpg?1783928253"
        ruling(
            "2021-02-05",
            "While in your library, a double-faced card has only the name of its front face. It's not " +
                "possible to reveal two of the same double-faced card this way."
        )
        ruling(
            "2021-02-05",
            "If you choose not to search your library, no cards are revealed or put anywhere else, and " +
                "you don't shuffle."
        )
    }
}
