package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Power Word Kill
 * {1}{B}
 * Instant
 * Destroy target non-Angel, non-Demon, non-Devil, non-Dragon creature.
 *
 * Each exclusion is its own `notSubtype` predicate, ANDed together, so a creature with any one of the
 * four types (a Demon Rogue, say) is not a legal target.
 */
val PowerWordKill = card("Power Word Kill") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target non-Angel, non-Demon, non-Devil, non-Dragon creature."

    spell {
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature
                    .notSubtype(Subtype.ANGEL)
                    .notSubtype(Subtype.DEMON)
                    .notSubtype(Subtype.DEVIL)
                    .notSubtype(Subtype.DRAGON)
            )
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Izzy"
        flavorText = "Aramil issued a long, heroic challenge, then the lich felled him with a single word."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/395b6ce4-143f-4eed-b565-98aa3d6208ef.jpg?1783926493"
        ruling("2021-07-23", "A creature is a legal target for Power Word Kill if it doesn't have any of these four creature types. If it has one of these four creature types, it's not a legal target, even if it also has a creature type that's not among these four. For example, a Demon Rogue is not a valid target for this spell.")
    }
}
