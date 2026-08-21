package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Gleeful Sabotage
 * {1}{G}
 * Sorcery
 *
 * Destroy target artifact or enchantment.
 * Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)
 *
 * - "artifact or enchantment" is one target slot with a disjunctive card predicate
 *   (`GameObjectFilter.Artifact or GameObjectFilter.Enchantment`), not two requirements — an
 *   artifact enchantment satisfies it once.
 * - [Effects.Destroy] is the move-to-graveyard-by-destruction shape, so indestructible,
 *   regeneration and "dies" triggers all behave (a plain move would bypass them).
 * - Conspire is declared as [KeywordAbility.Conspire] only; `CardBuilder` derives
 *   `Keyword.CONSPIRE` into `keywords`, so a separate `keywords(...)` line would be redundant.
 */
val GleefulSabotage = card("Gleeful Sabotage") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact or enchantment.\n" +
        "Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)"

    keywordAbility(KeywordAbility.Conspire)

    spell {
        val t = target(
            "target",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment))
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1b7d043-5f52-4df6-8dcf-5174a5b0c9cc.jpg?1783942743"
    }
}
