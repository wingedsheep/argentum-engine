package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Merciless Repurposing — March of the Machine #117
 * {4}{B}{B} · Instant
 *
 * Exile target creature. Incubate 3.
 *
 * Per the 2023-04-14 ruling, an illegal target on resolution fizzles the whole spell, so the
 * incubate rides the same resolution as the exile — nothing is created on a fizzle.
 */
val MercilessRepurposing = card("Merciless Repurposing") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature. Incubate 3. (Create an Incubator token with three +1/+1 " +
        "counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian " +
        "artifact creature.)"

    spell {
        val creature = target(TargetFilter(GameObjectFilter.Creature))
        effect = Effects.Exile(creature) then Effects.Incubate(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Artur Nakhodkin"
        flavorText = "As the harvesters bore away the last of his limbs, what remained of Urabrask " +
            "heard Elesh Norn say, \"Leave the traitor be.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70edec35-1770-47f0-9ad2-32e597ee0327.jpg?1783917004"
        ruling(
            "2023-04-14",
            "If the target of Merciless Repurposing is illegal as the spell tries to resolve, it won't " +
                "resolve and none of its effects will happen. You won't incubate."
        )
    }
}
