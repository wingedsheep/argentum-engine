package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dubious Challenge — Kaladesh #152
 * {3}{G} · Sorcery · Rare
 *
 * Look at the top ten cards of your library, exile up to two creature cards from among them,
 * then shuffle. Target opponent may choose one of the exiled cards and put it onto the
 * battlefield under their control. Put the rest onto the battlefield under your control.
 *
 * A private look at the top ten, from which the caster exiles up to two creature cards (face
 * up, so the opponent sees them while deciding); the unexiled cards never leave the library and
 * are shuffled back with it. The targeted opponent ([Chooser.TargetPlayer] — the spell's only
 * target) then picks up to one of the cards exiled this way; that card enters under the
 * opponent's control and whatever they left enters under the caster's. Choosing none, or the
 * caster exiling only one card or none, all fall out of the up-to selections.
 */
val DubiousChallenge = card("Dubious Challenge") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top ten cards of your library, exile up to two creature cards " +
        "from among them, then shuffle. Target opponent may choose one of the exiled cards and " +
        "put it onto the battlefield under their control. Put the rest onto the battlefield " +
        "under your control."

    spell {
        val opponent = target(Targets.Opponent)

        effect = Effects.Pipeline {
            val topTen = gather(CardSource.TopOfLibrary(10))
            val creatures = chooseUpTo(
                2,
                from = topTen,
                filter = GameObjectFilter.Creature,
                prompt = "Exile up to two creature cards",
                showAllCards = true
            )
            val exiled = moveTracked(creatures, CardDestination.ToZone(Zone.EXILE))
            run(Effects.ShuffleLibrary())
            val (theirs, yours) = chooseUpToSplit(
                1,
                from = exiled,
                chooser = Chooser.TargetPlayer,
                prompt = "You may choose one of the exiled cards to put onto the battlefield under your control",
                selectedLabel = "Under your control",
                remainderLabel = "Under their control"
            )
            move(theirs, CardDestination.ToZone(Zone.BATTLEFIELD, opponent.asPlayer))
            move(yours, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92965a78-277d-4a27-8174-fc2564bd1ee3.jpg?1783937180"
        ruling("2016-09-20", "The creature cards you exile are face up. Your opponent can see them while deciding whether to take one and which to take.")
        ruling("2016-09-20", "You may choose to exile only one creature card. If you do, the target opponent chooses who gets the card.")
        ruling("2016-09-20", "You may choose to exile no creature cards, even if there are one or more among the top ten cards of your library.")
        ruling("2016-09-20", "The cards that you look at but don't exile never leave your library. They're shuffled along with the rest of your library.")
        ruling("2016-09-20", "In a multiplayer game, you must choose the target opponent before you look at your top ten cards and decide what to exile.")
    }
}
