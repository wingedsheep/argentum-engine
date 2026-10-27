package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pyre of Heroes
 * {2}
 * Artifact
 * {2}, {T}, Sacrifice a creature: Search your library for a creature card that shares a creature
 * type with the sacrificed creature and has mana value equal to 1 plus that creature's mana value.
 * Put that card onto the battlefield, then shuffle. Activate only as a sorcery.
 *
 * Both halves of the search filter read the sacrificed creature through
 * [EffectTarget.SacrificedAsCost] — the cost-payment snapshot, i.e. the creature as it last
 * existed on the battlefield (its projected creature types, so a sacrificed Changeling has every
 * creature type, and its mana value). A Changeling card in the library has every creature type
 * too (CR 702.73a).
 */
val PyreOfHeroes = card("Pyre of Heroes") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}, Sacrifice a creature: Search your library for a creature card that shares a " +
        "creature type with the sacrificed creature and has mana value equal to 1 plus that creature's " +
        "mana value. Put that card onto the battlefield, then shuffle. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature
                .sharingCreatureTypeWith(EffectTarget.SacrificedAsCost(0))
                .manaValueEqualsDynamic(DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)) + 1),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true,
        )
        timing = TimingRule.SorcerySpeed
        description = "{2}, {T}, Sacrifice a creature: Search your library for a creature card that shares " +
            "a creature type with the sacrificed creature and has mana value equal to 1 plus its mana value, " +
            "put it onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae9a8e44-f5de-497d-be48-adf1bcbaec97.jpg?1783928184"
        ruling(
            "2021-02-05",
            "Use the creature types and mana value of the creature you sacrificed as it existed on the " +
                "battlefield to determine what creature card you can find. Notably, if you sacrifice a " +
                "creature that's a double-faced permanent with its back face up, you'll use the " +
                "characteristics of the back face, not the front face."
        )
    }
}
