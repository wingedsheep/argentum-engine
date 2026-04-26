package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moonglove Extractor
 * {2}{B}
 * Creature — Elf Warlock
 * 2/1
 *
 * Whenever this creature attacks, you draw a card and lose 1 life.
 */
val MoongloveExtractor = card("Moonglove Extractor") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Elf Warlock"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks, you draw a card and lose 1 life."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.DrawCards(1)
            .then(Effects.LoseLife(1, EffectTarget.Controller))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Milivoj Ćeran"
        flavorText = "Concentrating moonglove flowers into deadly moonglow poison is a revered but fleeting position for Lorwyn elves."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8383e0ab-81b4-4a6b-b87b-dd9180dca1c2.jpg?1767658152"
    }
}
