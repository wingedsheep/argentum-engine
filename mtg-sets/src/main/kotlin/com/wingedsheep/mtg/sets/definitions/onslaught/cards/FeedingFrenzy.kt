package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Feeding Frenzy
 * {2}{B}
 * Instant
 * Target creature gets -X/-X until end of turn, where X is the number of Zombies on the battlefield.
 */
val FeedingFrenzy = card("Feeding Frenzy") {
    manaCost = "{2}{B}"
    typeLine = "Instant"
    oracleText = "Target creature gets -X/-X until end of turn, where X is the number of Zombies on the battlefield."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        val zombieCount = DynamicAmounts.creaturesWithSubtype(Subtype("Zombie"))
        val negativeZombieCount = DynamicAmount.Multiply(zombieCount, -1)
        effect = Effects.ModifyStats(
            power = negativeZombieCount,
            toughness = negativeZombieCount,
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Nelson DeCastro"
        flavorText = "It wasn't as much a strategy as a dim instinct to drown their prey."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6d74c30-ebca-4684-ad84-3ca19193ad88.jpg?1562934515"
    }
}
