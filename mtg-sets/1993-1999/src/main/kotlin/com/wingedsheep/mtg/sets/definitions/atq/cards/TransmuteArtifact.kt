package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

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
        effect = Effects.Pipeline {
            // Sacrifice an artifact you control (storing the choice so its mana value can be
            // compared later). Scoped with youControl() — you can only sacrifice permanents you
            // control (CR 701.21a), and BattlefieldMatching defaults to all players' battlefields.
            val sacrificeable = gather(
                CardSource.BattlefieldMatching(filter = GameObjectFilter.Artifact.youControl())
            )
            val sacrificed = chooseExactly(1, from = sacrificeable, prompt = "Sacrifice an artifact")
            sacrifice(sacrificed)
            // If you sacrificed one, search and (conditionally) put the found artifact into play.
            run(Effects.If(
                condition = whenMatches(sacrificed),
                then = Effects.Pipeline {
                    val searchable = gather(
                        CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Artifact),
                        search = true
                    )
                    val found = chooseUpTo(1, from = searchable, prompt = "Search your library for an artifact card")
                    run(Effects.If(
                        condition = Conditions.CompareAmounts(
                                left = DynamicAmounts.manaValueOf(found),
                                operator = ComparisonOperator.LTE,
                                right = DynamicAmounts.manaValueOf(sacrificed)
                            ),
                        then = Effects.Pipeline { move(found, CardDestination.ToZone(Zone.BATTLEFIELD)) },
                        // found MV > sacrificed MV → you may pay {X} = the difference.
                        otherwise = Effects.MayPay(
                            cost = Effects.PayDynamicMana(
                                    DynamicAmounts.manaValueOf(found) - DynamicAmounts.manaValueOf(sacrificed)
                                ),
                            then = Effects.Pipeline { move(found, CardDestination.ToZone(Zone.BATTLEFIELD)) },
                            otherwise = Effects.Pipeline { toGraveyard(found) }
                        )
                    ))
                }
            ))
            // Then shuffle.
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6eab6765-eba3-4844-81ca-ae37a6e903df.jpg?1562918256"
    }
}
