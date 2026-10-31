package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource

/**
 * Whirlwind Denial
 * {2}{U}
 * Instant
 * For each spell and ability your opponents control, counter it unless its controller pays {4}.
 *
 * Untargeted: [CounterTargetSource.EachOnStack] gathers every opponent's spell and ability on
 * the stack. Opponents decide in APNAP order, then every unpaid object is countered at once.
 */
val WhirlwindDenial = card("Whirlwind Denial") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "For each spell and ability your opponents control, counter it unless its controller pays {4}."

    spell {
        effect = Effects.CounterUnlessPays(
            "{4}",
            target = CounterTarget.SpellOrAbility,
            targetSource = CounterTargetSource.EachOnStack()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Lie Setiawan"
        flavorText = "\"No, no, and ... no.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e127856-bedd-40a9-9e8e-d1f9fbefe07d.jpg?1783931573"
    }
}
