package com.wingedsheep.mtg.sets.definitions.inv.cards

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
 * Void
 * {3}{B}{R}
 * Sorcery
 * Choose a number. Destroy all artifacts and creatures with mana value equal to that
 * number. Then target player reveals their hand and discards all nonland cards with
 * mana value equal to the number.
 *
 * "Choose a number" is modeled with [Effects.ChooseNumberThen], which stamps the chosen
 * number onto the effect context as X. The board wipe and the discard both filter by
 * [CardPredicate.ManaValueEqualsX] (`manaValueEqualsX()`), so a single chosen value drives
 * both steps.
 */
val Void = card("Void") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Choose a number. Destroy all artifacts and creatures with mana value equal to " +
        "that number. Then target player reveals their hand and discards all nonland cards with " +
        "mana value equal to the number."

    spell {
        val targetPlayer = target("target player", TargetPlayer())
        effect = Effects.ChooseNumberThen(
            then = Effects.Composite(
                listOf(
                    // Destroy all artifacts and creatures with mana value equal to the chosen number.
                    Effects.DestroyAll(
                        filter = GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.Or(listOf(CardPredicate.IsArtifact, CardPredicate.IsCreature)),
                                CardPredicate.ManaValueEqualsX,
                            ),
                        ),
                    ),
                    // Target player reveals their hand and discards all nonland cards with that mana value.
                    RevealHandEffect(targetPlayer),
                    GatherCardsEffect(
                        source = CardSource.FromZone(
                            zone = Zone.HAND,
                            player = Player.ContextPlayer(0),
                            filter = GameObjectFilter(
                                cardPredicates = listOf(
                                    CardPredicate.IsNonland,
                                    CardPredicate.ManaValueEqualsX,
                                ),
                            ),
                        ),
                        storeAs = "voidDiscard",
                    ),
                    MoveCollectionEffect(
                        from = "voidDiscard",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                        moveType = MoveType.Discard,
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "287"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62dc1df7-b9db-4f5f-a340-08287cd3d9e5.jpg?1562915020"
    }
}
