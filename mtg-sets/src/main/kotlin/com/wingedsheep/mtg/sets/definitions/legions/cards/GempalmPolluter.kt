package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Gempalm Polluter
 * {5}{B}
 * Creature — Zombie
 * 4/3
 * Cycling {B}{B}
 * When you cycle Gempalm Polluter, you may have target player lose life equal to
 * the number of Zombies on the battlefield.
 */
val GempalmPolluter = card("Gempalm Polluter") {
    manaCost = "{5}{B}"
    typeLine = "Creature — Zombie"
    oracleText = "Cycling {B}{B}\nWhen you cycle Gempalm Polluter, you may have target player lose life equal to the number of Zombies on the battlefield."
    power = 4
    toughness = 3

    keywordAbility(KeywordAbility.cycling("{B}{B}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val player = target("target player", Targets.Player)
        effect = MayEffect(
            LoseLifeEffect(
                DynamicAmounts.creaturesWithSubtype(Subtype("Zombie")),
                player
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e9943ac-9e3f-4ee0-b5fd-3b0fb17097d8.jpg?1562923789"
    }
}
