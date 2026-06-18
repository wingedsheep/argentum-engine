package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Transmute Artifact
 * {U}{U}
 * Sorcery
 * Sacrifice an artifact. If you do, search your library for an artifact card. If that card's mana
 * value is less than or equal to the sacrificed artifact's mana value, put it onto the battlefield.
 * If it's greater, you may pay {X}, where X is the difference. If you do, put it onto the
 * battlefield. If you don't, put it into its owner's graveyard. Then shuffle.
 *
 * Composed entirely from existing pipeline atoms — no new engine type:
 *  1. Gather artifacts you control → select exactly one ("sacrificed") and sacrifice it
 *     (`MoveType.Sacrifice`, owner's graveyard). The collection retains the entity id, so its mana
 *     value is still readable afterward via `StoredCardManaValue("sacrificed")`.
 *  2. Gated on having actually sacrificed one (`CollectionContainsMatch("sacrificed")` — the "if
 *     you do" leg): search your library, choosing up to one artifact card into "found" (without
 *     moving it yet), then branch on the mana-value comparison:
 *       - found MV ≤ sacrificed MV  → put "found" onto the battlefield.
 *       - else (found MV > sacrificed MV) → you may pay {X} = the difference; if you do, put it
 *         onto the battlefield; if you don't, put it into its owner's graveyard.
 *  3. Shuffle (always, since you searched your library).
 *
 * With no artifact to sacrifice, the "if you do" gate fails and the spell does nothing further;
 * with an empty selection the "found" collection is empty and the moves are no-ops.
 */
val TransmuteArtifact = card("Transmute Artifact") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Sacrifice an artifact. If you do, search your library for an artifact card. If that card's mana value is less than or equal to the sacrificed artifact's mana value, put it onto the battlefield. If it's greater, you may pay {X}, where X is the difference. If you do, put it onto the battlefield. If you don't, put it into its owner's graveyard. Then shuffle."

    spell {
        effect = Effects.Composite(listOf(
            // Sacrifice an artifact (storing the choice so its mana value can be compared later).
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(filter = GameObjectFilter.Artifact),
                storeAs = "sacrificeable"
            ),
            SelectFromCollectionEffect(
                from = "sacrificeable",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "sacrificed",
                prompt = "Sacrifice an artifact"
            ),
            MoveCollectionEffect(
                from = "sacrificed",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Sacrifice
            ),
            // If you sacrificed one, search and (conditionally) put the found artifact into play.
            GatedEffect(
                gate = Gate.WhenCondition(CollectionContainsMatch("sacrificed")),
                then = Effects.Composite(listOf(
                    GatherCardsEffect(
                        source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Artifact),
                        storeAs = "searchable"
                    ),
                    SelectFromCollectionEffect(
                        from = "searchable",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        storeSelected = "found",
                        prompt = "Search your library for an artifact card"
                    ),
                    GatedEffect(
                        // found MV ≤ sacrificed MV → free to battlefield.
                        gate = Gate.WhenCondition(
                            Compare(
                                left = DynamicAmount.StoredCardManaValue("found"),
                                operator = ComparisonOperator.LTE,
                                right = DynamicAmount.StoredCardManaValue("sacrificed")
                            )
                        ),
                        then = MoveCollectionEffect(
                            from = "found",
                            destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                        ),
                        // found MV > sacrificed MV → you may pay {X} = the difference.
                        otherwise = GatedEffect(
                            gate = Gate.MayPay(
                                Effects.PayDynamicMana(
                                    DynamicAmount.Subtract(
                                        DynamicAmount.StoredCardManaValue("found"),
                                        DynamicAmount.StoredCardManaValue("sacrificed")
                                    )
                                )
                            ),
                            then = MoveCollectionEffect(
                                from = "found",
                                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                            ),
                            otherwise = MoveCollectionEffect(
                                from = "found",
                                destination = CardDestination.ToZone(Zone.GRAVEYARD)
                            )
                        )
                    )
                ))
            ),
            // Then shuffle.
            ShuffleLibraryEffect()
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6eab6765-eba3-4844-81ca-ae37a6e903df.jpg?1562918256"
    }
}
