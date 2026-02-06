package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.OnCycle

/**
 * Slice and Dice
 * {4}{R}{R}
 * Sorcery
 * Slice and Dice deals 4 damage to each creature.
 * Cycling {2}{R}
 * When you cycle Slice and Dice, you may have it deal 1 damage to each creature.
 */
val SliceAndDice = card("Slice and Dice") {
    manaCost = "{4}{R}{R}"
    typeLine = "Sorcery"

    spell {
        effect = DealDamageToGroupEffect(4)
    }

    keywordAbility(KeywordAbility.cycling("{2}{R}"))

    triggeredAbility {
        trigger = OnCycle(controllerOnly = true)
        effect = MayEffect(DealDamageToGroupEffect(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Matt Cavotta"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e5a2a6d-3966-4514-b425-e4af818ab808.jpg?1562920062"
    }
}
