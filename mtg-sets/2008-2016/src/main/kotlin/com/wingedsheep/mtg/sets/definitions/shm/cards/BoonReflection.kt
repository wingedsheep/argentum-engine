package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Boon Reflection
 * {4}{W}
 * Enchantment
 * If you would gain life, you gain twice that much life instead.
 *
 * Rhox Faithmender's replacement: [ModifyLifeGain] with `multiplier = 2`, scoped to life you gain.
 * Each copy applies in turn, so two Boon Reflections multiply by four (ruling).
 */
val BoonReflection = card("Boon Reflection") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If you would gain life, you gain twice that much life instead."

    replacementEffect(
        ModifyLifeGain(
            multiplier = 2,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Terese Nielsen & Ron Spencer"
        flavorText = "Kithkin healers chant the clan songs of both their parents over the broth to double its curative effect."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a27ba4d-53af-427d-9e51-be04db077fac.jpg?1783942769"
        ruling("2020-08-07", "The effects of multiple Boon Reflections are cumulative. For example, if you control two Boon Reflections, you'll gain four times the original amount of life. If you control three Boon Reflections, you'll gain eight times the original amount, and so on.")
        ruling("2020-08-07", "If an effect would set your life total to a specific number that's higher than your current life total, that effect would cause you to gain life equal to the difference. Boon Reflection will then double the amount of life that effect would cause you to gain. For example, if you have 3 life and an effect says that your life total \"becomes 10,\" your life total will actually become 17.")
    }
}
