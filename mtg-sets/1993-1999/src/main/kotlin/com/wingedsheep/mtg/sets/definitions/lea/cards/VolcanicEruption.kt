package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Volcanic Eruption
 * {X}{U}{U}{U}
 * Sorcery
 * Destroy X target Mountains. Volcanic Eruption deals damage to each creature and each player
 * equal to the number of Mountains put into a graveyard this way.
 *
 * Modeling notes:
 *  - "X target Mountains" is exactly X (`targets(…, exactly = X)`, Builder's Bane's shape). X = 0
 *    is castable with no targets (2004-10-04 ruling), and a partially-illegal target set still
 *    resolves for the rest (CR 608.2b).
 *  - The damage counts Mountains *actually put into a graveyard*, not the announced X: the
 *    targets are destroyed with `moveTracked` (an indestructible or regenerated Mountain never
 *    moves), and the moved set is then re-read with `currentlyIn(GRAVEYARD)` so a Mountain whose
 *    destruction was redirected elsewhere (e.g. to exile) doesn't count (Silvan Reveler's shape).
 *  - The count is frozen with `storeNumber` before the damage passes, then dealt to each creature
 *    and each player — Ashling the Pilgrim's / Pestilence's "each creature and each player" pair.
 */
val VolcanicEruption = card("Volcanic Eruption") {
    manaCost = "{X}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Destroy X target Mountains. Volcanic Eruption deals damage to each creature and " +
        "each player equal to the number of Mountains put into a graveyard this way."

    spell {
        targets(
            TargetFilter(GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)),
            exactly = DynamicAmounts.xValue()
        )
        effect = Effects.Pipeline {
            val mountains = gather(CardSource.ChosenTargets)
            val destroyed = moveTracked(
                mountains,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
            val inGraveyard = filter(destroyed, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD))
            val eruptionDamage = storeNumber(inGraveyard.count)
            run(Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(eruptionDamage.amount, EffectTarget.IterationEntity)
            ))
            run(Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealDamage(eruptionDamage.amount, EffectTarget.Controller))
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a80582b1-09db-45f8-b362-0e5207a5a8e6.jpg?1783948698"

        ruling("2004-10-04", "Can be used with X equal to zero. This is useful if no Mountains are on the battlefield.")
    }
}
