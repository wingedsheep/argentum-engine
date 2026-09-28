package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Change the Equation
 * {1}{U}
 * Instant
 * Choose one —
 * • Counter target spell with mana value 2 or less.
 * • Counter target red or green spell with mana value 6 or less.
 *
 * Both restrictions are targeting restrictions, so they live on the mode's target filter rather
 * than in a resolution-time condition: a spell outside the range is simply not a legal target.
 */
val ChangeTheEquation = card("Change the Equation") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target spell with mana value 2 or less.\n" +
        "• Counter target red or green spell with mana value 6 or less."

    spell {
        modal(chooseCount = 1) {
            mode("Counter target spell with mana value 2 or less") {
                target(TargetFilter.SpellOnStack.manaValueAtMost(2))
                effect = Effects.CounterSpell()
            }
            mode("Counter target red or green spell with mana value 6 or less") {
                target(TargetFilter.SpellOnStack.withAnyColor(Color.RED, Color.GREEN).manaValueAtMost(6))
                effect = Effects.CounterSpell()
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Alix Branwyn"
        flavorText = "\"It's not your fault. Strixhaven taught you an imperfect premise. Okay, the " +
            "imperfect execution was your fault.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e98db9ed-b43f-4bc1-b7a9-ce03a534d992.jpg?1783917042"
    }
}
