package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Aethertow
 * {3}{W/U}
 * Instant
 *
 * Put target attacking or blocking creature on top of its owner's library.
 * Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)
 *
 * - The target is a single [com.wingedsheep.sdk.scripting.targets.TargetObject] whose filter carries
 *   the "attacking *or* blocking" disjunction as one state predicate
 *   ([TargetFilter.AttackingOrBlockingCreature]) rather than two requirements — the spell targets one
 *   creature, and the creature has to satisfy either half.
 * - Conspire is declared as [KeywordAbility.Conspire] only. `CardBuilder` derives
 *   `Keyword.CONSPIRE` into `keywords` from the keyword ability, so declaring it a second time via
 *   `keywords(...)` would be redundant.
 * - The copy chooses its own target, so a creature that stops attacking or blocking in between (or
 *   the copy resolving first and moving the original target) is handled by ordinary target legality,
 *   not by anything this card models.
 */
val Aethertow = card("Aethertow") {
    manaCost = "{3}{W/U}"
    typeLine = "Instant"
    oracleText = "Put target attacking or blocking creature on top of its owner's library.\n" +
        "Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)"

    keywordAbility(KeywordAbility.Conspire)

    spell {
        val creature = target(
            "target",
            TargetCreature(filter = TargetFilter.AttackingOrBlockingCreature)
        )
        effect = Effects.PutOnTopOfLibrary(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72a656e7-9c1c-40b6-91fe-0098f72d8384.jpg?1783942738"
    }
}
