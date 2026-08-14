package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hex Magic — Marvel Super Heroes #133 (uncommon)
 * {2}{R} · Sorcery — Arcane
 *
 * Exile all the cards from your hand, then draw that many cards. Until the end of your next turn,
 * you may play cards exiled this way.
 *
 * A three-step Gather → Move → Grant pipeline, no new vocabulary:
 *
 *  1. `gather(CardSource.FromZone(HAND, You))` snapshots your hand *as the spell resolves*. Hex
 *     Magic itself is on the stack at that moment, so it isn't caught by its own gather.
 *  2. `exile(...)` moves that whole collection to exile.
 *  3. "draw that many" reads the size of the collection —
 *     [DynamicAmount.DistinctEntitiesInCollections] over the single slot — so it's the number of
 *     cards actually exiled, evaluated after the move (the collection tracks entity ids, which
 *     survive the zone change). An empty hand draws zero rather than erroring.
 *  4. [GrantMayPlayFromExileEffect] with [MayPlayExpiry.UntilEndOfNextTurn] gives the
 *     "until the end of your next turn, you may play cards exiled this way" permission, scoped to
 *     that same collection. Those cards still cost their mana and obey normal timing.
 *
 * "Sorcery — Arcane" needs no SDK support: `typeLine` is free-form, and nothing in this set keys
 * off Arcane (splice onto Arcane is not part of Marvel Super Heroes).
 */
val HexMagic = card("Hex Magic") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery — Arcane"
    oracleText = "Exile all the cards from your hand, then draw that many cards. Until the end of " +
        "your next turn, you may play cards exiled this way."

    spell {
        effect = Effects.Pipeline(
            descriptionOverride = "Exile all the cards from your hand, then draw that many cards. " +
                "Until the end of your next turn, you may play cards exiled this way."
        ) {
            val exiled = gather(
                CardSource.FromZone(Zone.HAND, Player.You),
                name = "hexMagicExiled",
            )

            exile(exiled)

            run(
                Effects.DrawCards(
                    DynamicAmount.DistinctEntitiesInCollections(listOf(exiled.key))
                )
            )

            run(GrantMayPlayFromExileEffect(exiled.key, MayPlayExpiry.UntilEndOfNextTurn))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Kevin Glint"
        flavorText = "At the Scarlet Witch's touch, probabilities shift and the impossible " +
            "becomes real."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/259bed47-1950-43ae-8efd-3009537529a8.jpg?1783902932"
    }
}
