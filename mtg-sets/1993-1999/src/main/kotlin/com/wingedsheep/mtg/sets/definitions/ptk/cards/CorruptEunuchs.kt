package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Corrupt Eunuchs
 * {3}{R}
 * Creature — Human Advisor
 * 2/2
 * When this creature enters, it deals 2 damage to target creature.
 */
val CorruptEunuchs = card("Corrupt Eunuchs") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Advisor"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, it deals 2 damage to target creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Li Yousong"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e084664-2faf-4f7a-9068-d58d3f4b8456.jpg"
    }
}
