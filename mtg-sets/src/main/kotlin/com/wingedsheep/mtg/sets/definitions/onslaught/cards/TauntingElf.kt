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

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MustBeBlockedEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "286"
        artist = "Christopher Rush"
        flavorText = "\"The bravest hero is the one who can make the enemy forget about everyone else.\"\n—Elvish warrior"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b24af94-0a93-40ed-8e34-8e62ddb8b650.jpg?1562920244"
    }
}
