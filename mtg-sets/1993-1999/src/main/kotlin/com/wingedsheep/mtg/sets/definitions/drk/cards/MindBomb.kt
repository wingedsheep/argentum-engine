package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mind Bomb
 * {U}
 * Sorcery
 * Each player may discard up to three cards. Mind Bomb deals damage to each player equal to
 * 3 minus the number of cards they discarded this way.
 *
 * A per-player loop rather than one global step, because each player's damage is computed from
 * *their own* discard. `ForEachPlayerEffect` rebinds `Player.You` to the current player and hands
 * each iteration a fresh set of collections, so the count read at the end of one player's body can't
 * leak into the next player's.
 *
 * "Up to three" is `SelectionMode.ChooseUpTo(3)` with the player themselves as chooser — declining
 * is discarding zero, which is the maximum damage rather than an illegal choice. The damage is
 * `3 − |discarded|`, floored by the engine at zero, so a player who discards three takes none.
 *
 * The move is `MoveType.Discard`, so "whenever you discard a card" triggers and Madness see it.
 */
val MindBomb = card("Mind Bomb") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player may discard up to three cards. Mind Bomb deals damage to each " +
        "player equal to 3 minus the number of cards they discarded this way."

    spell {
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any),
                    storeAs = "mindBombCandidates",
                ),
                SelectFromCollectionEffect(
                    from = "mindBombCandidates",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(3)),
                    chooser = Chooser.Controller,
                    storeSelected = "mindBombDiscarded",
                    prompt = "Discard up to three cards?",
                ),
                MoveCollectionEffect(
                    from = "mindBombDiscarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                    moveType = MoveType.Discard,
                ),
                Effects.DealDamage(
                    DynamicAmount.Subtract(
                        DynamicAmount.Fixed(3),
                        DynamicAmount.DistinctEntitiesInCollections(listOf("mindBombDiscarded")),
                    ),
                    EffectTarget.PlayerRef(Player.You),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ee810a5-f0f9-4b73-8194-3d1344784050.jpg?1783947942"
    }
}
