package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spare from Evil
 * {1}{W}
 * Instant
 * Creatures you control gain protection from non-Human creatures until end of turn.
 *
 * The affected set is locked in at resolution (CR 611.2c), so it fans out over the creatures you
 * control with [Effects.ForEachInGroup]; the quality is the complement of a subtype narrowed to a
 * card type — `ProtectionScope.Subtype("Human", "Creature", negated = true)`.
 */
val SpareFromEvil = card("Spare from Evil") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control gain protection from non-Human creatures until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantProtectionFromSubtype(
                "Human",
                EffectTarget.IterationEntity,
                cardType = "Creature",
                negated = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Jason Felix"
        flavorText = "\"As long as we can utter a prayer or lift a weapon, we have the power to fight back.\"\n—Thalia, Knight-Cathar"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d01b5d97-b5ae-42a7-944a-feb12febd63c.jpg?1783940984"
    }
}
