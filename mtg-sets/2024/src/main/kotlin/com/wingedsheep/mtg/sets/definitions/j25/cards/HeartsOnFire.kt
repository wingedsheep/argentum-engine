package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hearts on Fire
 * {1}{R}
 * Instant
 *
 * One or two target creatures each get +2/+1 until end of turn.
 */
val HeartsOnFire = card("Hearts on Fire") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "One or two target creatures each get +2/+1 until end of turn."

    spell {
        target = TargetObject(filter = TargetFilter(GameObjectFilter.Creature), count = 2, minCount = 1)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(2, 1, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Eelis Kyttanen"
        flavorText = "\"With you by my side, I am unstoppable.\"\n—Halana, to Alena"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b10fcb27-1486-4cf1-8fde-eea6f799ab8b.jpg?1783908865"
    }
}
