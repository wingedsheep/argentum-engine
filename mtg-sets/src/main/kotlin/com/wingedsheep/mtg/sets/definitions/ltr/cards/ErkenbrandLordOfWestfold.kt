package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Erkenbrand, Lord of Westfold
 * {3}{R}
 * Legendary Creature — Human Soldier
 * 3/3
 *
 * Whenever Erkenbrand or another Human you control enters, creatures you control get +1/+0 until end of turn.
 */
val ErkenbrandLordOfWestfold = card("Erkenbrand, Lord of Westfold") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Whenever Erkenbrand or another Human you control enters, creatures you control get +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl().withSubtype("Human"),
            binding = TriggerBinding.ANY
        )
        effect = GroupPatterns.modifyStatsForAll(
            1, 0,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Alexander Mokhov"
        flavorText = "Down from the hills leaped Erkenbrand, lord of Westfold, and the hosts of Isengard roared in fear."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38f81e68-cddd-47f4-b1d1-a297c3298c25.jpg?1686968892"
    }
}
