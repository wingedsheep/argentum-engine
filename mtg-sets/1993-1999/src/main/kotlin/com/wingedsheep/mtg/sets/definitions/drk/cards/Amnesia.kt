package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Amnesia
 * {3}{U}{U}{U}
 * Sorcery
 * Target player reveals their hand and discards all nonland cards.
 *
 * The Gather → Move half of the standard pipeline, with no Select step because the card discards
 * *all* of what it gathers. Invasion's Void is the same shape with a mana-value clause added to the
 * filter. The reveal is a real [RevealHandEffect] rather than cosmetic: the discard is public
 * information, and the hand is revealed before any of it moves.
 *
 * `MoveType.Discard` rather than a plain zone move, so discard triggers (Madness, "whenever you
 * discard a card") see it as the discard it is.
 */
val Amnesia = card("Amnesia") {
    manaCost = "{3}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand and discards all nonland cards."

    spell {
        val targetPlayer = target("target player", TargetPlayer())
        effect = Effects.Composite(
            RevealHandEffect(targetPlayer),
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.HAND,
                    player = Player.ContextPlayer(0),
                    filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNonland)),
                ),
                storeAs = "amnesiaDiscard",
            ),
            MoveCollectionEffect(
                from = "amnesiaDiscard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Mark Poole"
        flavorText = "\"When one has witnessed the unspeakable, 'tis sometimes better to forget.\" " +
            "—Vervamon the Elder"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e07df65c-ebcc-4873-b928-d99040d1f2f6.jpg?1783947945"
    }
}
