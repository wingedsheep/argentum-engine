package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ankle Shanker
 * {2}{R}{W}{B}
 * Creature — Goblin Berserker
 * 2/2
 * Haste
 * Whenever Ankle Shanker attacks, creatures you control gain first strike
 * and deathtouch until end of turn.
 */
val AnkleShanker = card("Ankle Shanker") {
    manaCost = "{2}{R}{W}{B}"
    typeLine = "Creature — Goblin Berserker"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhenever Ankle Shanker attacks, creatures you control gain first strike and deathtouch until end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = EffectPatterns.grantKeywordToAll(
            keyword = Keyword.FIRST_STRIKE,
            filter = GroupFilter.AllCreaturesYouControl
        ).then(
            EffectPatterns.grantKeywordToAll(
                keyword = Keyword.DEATHTOUCH,
                filter = GroupFilter.AllCreaturesYouControl
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Zoltan Boros"
        flavorText = "The stature of the fighter matters less than the depth of the cut."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c5bb5d2-181a-44ea-9b56-87f8d96bf634.jpg?1562788164"
    }
}
