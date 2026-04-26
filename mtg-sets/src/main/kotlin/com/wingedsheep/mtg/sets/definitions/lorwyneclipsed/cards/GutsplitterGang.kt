package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gutsplitter Gang
 * {3}{B}
 * Creature — Goblin Berserker
 * 6/6
 *
 * At the beginning of your first main phase, you may blight 2. If you don't,
 * you lose 3 life. (To blight 2, put two -1/-1 counters on a creature you control.)
 */
val GutsplitterGang = card("Gutsplitter Gang") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Goblin Berserker"
    power = 6
    toughness = 6
    oracleText = "At the beginning of your first main phase, you may blight 2. " +
        "If you don't, you lose 3 life. " +
        "(To blight 2, put two -1/-1 counters on a creature you control.)"

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = OptionalCostEffect(
            cost = EffectPatterns.blight(2),
            ifPaid = CompositeEffect(emptyList()),
            ifNotPaid = Effects.LoseLife(3, EffectTarget.Controller),
            descriptionOverride = "You may blight 2. If you don't, you lose 3 life"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Tyler Walpole"
        flavorText = "Known for being as ferocious in challenging each other for the first swing as they are against the enemy."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9ff74349-693d-4373-a194-9796316dd1f1.jpg?1767732693"
    }
}
