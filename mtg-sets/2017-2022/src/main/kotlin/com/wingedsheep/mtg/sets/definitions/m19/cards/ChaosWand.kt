package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chaos Wand — Core Set 2019 #228
 * {3} · Artifact · Rare
 *
 * {4}, {T}: Target opponent exiles cards from the top of their library until they exile an
 * instant or sorcery card. You may cast that card without paying its mana cost. Then put the
 * exiled cards that weren't cast this way on the bottom of that library in a random order.
 *
 * Unpredictable Cyclone's "exile until, may cast free, rest to the bottom at random" pipeline,
 * walked over the target opponent's library (Nicol Bolas, God-Pharaoh's
 * `gatherUntilMatch(..., Player.TargetOpponent)`). The cast happens during resolution, by the
 * Wand's controller; the cast card has left exile, so only the cards still in exile go back to
 * the bottom of that opponent's library.
 */
val ChaosWand = card("Chaos Wand") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{4}, {T}: Target opponent exiles cards from the top of their library until they exile an " +
        "instant or sorcery card. You may cast that card without paying its mana cost. Then put the exiled " +
        "cards that weren't cast this way on the bottom of that library in a random order."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val (found, exiled) = gatherUntilMatch(GameObjectFilter.InstantOrSorcery, player = Player.TargetOpponent)
            exile(exiled)
            ifNotEmpty(found) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
            }
            val notCast = filter(exiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            toLibraryBottom(notCast, player = Player.TargetOpponent, order = CardOrder.Random)
        }
        description = "{4}, {T}: Target opponent exiles cards from the top of their library until they exile an " +
            "instant or sorcery card. You may cast that card without paying its mana cost. Then put the exiled " +
            "cards that weren't cast this way on the bottom of that library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Kari Christensen"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62ff0730-6dd9-42d2-be0d-655d04adf229.jpg?1783934515"
        ruling("2018-07-13", "If the opponent exiles their entire library without exiling an instant or sorcery card, that player stops exiling cards, and the exiled cards are put into their library in a random order.")
        ruling("2018-07-13", "If you don't cast the exiled instant or sorcery card, it's put on the bottom of that library with the other cards.")
        ruling("2018-07-13", "If you wish to cast the exiled card, you must cast it while Chaos Wand's ability is resolving. You can't cast it later in the turn. A spell cast this way may be cast at a time you normally wouldn't be able to cast a spell of that type, but other restrictions (such as \"Cast this spell only during combat\") are enforced.")
        ruling("2018-07-13", "If the exiled card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
