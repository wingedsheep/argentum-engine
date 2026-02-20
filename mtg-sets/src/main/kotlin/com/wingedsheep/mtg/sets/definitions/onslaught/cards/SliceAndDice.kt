package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.Triggers

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
    oracleText = "Slice and Dice deals 4 damage to each creature.\nCycling {2}{R}\nWhen you cycle Slice and Dice, you may have it deal 1 damage to each creature."

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures, DealDamageEffect(4, EffectTarget.Self))
    }

    keywordAbility(KeywordAbility.cycling("{2}{R}"))

    triggeredAbility {
        trigger = Triggers.YouCycle
        effect = MayEffect(ForEachInGroupEffect(GroupFilter.AllCreatures, DealDamageEffect(1, EffectTarget.Self)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Mark Brill"
        flavorText = ""
        imageUri = "https://gatherer-static.wizards.com/Cards/medium/D51089E294D45760206DB9B12C753751003CDF5B5E74140CB66F52E9AD3FEA17.webp"
    }
}
