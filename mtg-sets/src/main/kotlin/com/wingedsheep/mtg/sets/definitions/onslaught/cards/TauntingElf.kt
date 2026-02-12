package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MustBeBlockedEffect

/**
 * Taunting Elf
 * {G}
 * Creature — Elf
 * 0/1
 * All creatures able to block Taunting Elf do so.
 */
val TauntingElf = card("Taunting Elf") {
    manaCost = "{G}"
    typeLine = "Creature — Elf"
    power = 0
    toughness = 1
    oracleText = "All creatures able to block Taunting Elf do so."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MustBeBlockedEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "290"
        artist = "Rebecca Guay"
        flavorText = "The safety of the village depends on the beast thinking with its stomach."
        imageUri = "https://cards.scryfall.io/large/front/6/b/6b24af94-9632-47da-9bf3-e81bb743cd43.jpg?1562920244"
    }
}
