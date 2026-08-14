package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * Sandman's Quicksand — Marvel's Spider-Man #63
 * {1}{B}{B} · Sorcery
 *
 * Mayhem {3}{B}
 * All creatures get -2/-2 until end of turn. If this spell's mayhem cost was paid, creatures
 * your opponents control get -2/-2 until end of turn instead.
 *
 * The mayhem rider reads [Conditions.MayhemCostWasPaid] (CR 702.187) off the resolution context —
 * for a sorcery that flag is carried on the resolving spell, not a permanent.
 */
val SandmansQuicksand = card("Sandman's Quicksand") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Mayhem {3}{B} (You may cast this card from your graveyard for {3}{B} if you " +
        "discarded it this turn. Timing rules still apply.)\n" +
        "All creatures get -2/-2 until end of turn. If this spell's mayhem cost was paid, " +
        "creatures your opponents control get -2/-2 until end of turn instead."

    spell {
        effect = ConditionalEffect(
            condition = Conditions.MayhemCostWasPaid,
            effect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesOpponentsControl,
                effect = ModifyStatsEffect(-2, -2, EffectTarget.Self)
            ),
            elseEffect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreatures,
                effect = ModifyStatsEffect(-2, -2, EffectTarget.Self)
            )
        )
    }

    mayhem("{3}{B}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7795e17-6717-464c-9ae3-20da52ba005a.jpg?1783905343"
    }
}
