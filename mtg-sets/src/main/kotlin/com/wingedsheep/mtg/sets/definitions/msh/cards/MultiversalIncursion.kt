package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Multiversal Incursion — Marvel Super Heroes #68 (mythic)
 * {5}{U}{U} · Sorcery
 *
 * For each nontoken creature you control, create a token that's a copy of that creature, except
 * it isn't legendary.
 *
 * The Second Harvest / Kindred Charge shape: [Effects.ForEachInGroup] snapshots the group before
 * any iteration (CR 611.2c), so the freshly created copies are never themselves re-iterated, and
 * inside the body [EffectTarget.Self] is the creature currently being iterated. The
 * `nontoken()` predicate is the printed restriction — token creatures you control are skipped.
 *
 * "Except it isn't legendary" is the copy modifier [Effects.CreateTokenCopyOfTarget]'s
 * `removedSupertypes`: the token copies the creature's copiable characteristics (CR 707.2) minus
 * the legendary supertype, so the legend rule never puts a copy and its original into the
 * graveyard.
 */
val MultiversalIncursion = card("Multiversal Incursion") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "For each nontoken creature you control, create a token that's a copy of that " +
        "creature, except it isn't legendary."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl().nontoken()),
            effect = Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.Self,
                removedSupertypes = setOf(Supertype.LEGENDARY)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "68"
        artist = "Lordigan"
        flavorText = "\"Dibs on not dying this time.\"\n—Spider-Man, Peter Parker"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a505f6f-933c-4644-838f-897f5e4133b0.jpg?1783902956"
    }
}
