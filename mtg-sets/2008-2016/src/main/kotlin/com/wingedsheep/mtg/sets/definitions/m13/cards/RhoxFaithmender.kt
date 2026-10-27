package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rhox Faithmender
 * {3}{W}
 * Creature — Rhino Monk
 * 1/5
 * Lifelink
 * If you would gain life, you gain twice that much life instead.
 *
 * The doubling is [ModifyLifeGain] with `multiplier = 2`, scoped to life you gain. Each copy is a
 * separate replacement applied in turn, so two Faithmenders multiply by four (ruling).
 */
val RhoxFaithmender = card("Rhox Faithmender") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rhino Monk"
    power = 1
    toughness = 5
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "If you would gain life, you gain twice that much life instead."

    keywords(Keyword.LIFELINK)

    replacementEffect(
        ModifyLifeGain(
            multiplier = 2,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Wesley Burt"
        flavorText = "Rhoxes who live long enough to retire from war lend support with their extensive knowledge of combat injuries."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85ea185a-7b38-49f3-be73-be8180fb6295.jpg?1783940514"
        ruling("2012-07-01", "If you control two Rhox Faithmenders, life you gain will be multiplied by four. Three Rhox Faithmenders will multiply any life gain by eight, and so on.")
        ruling("2012-07-01", "If an effect sets your life total to a specific number, and that number is higher than your current life total, the effect will cause you to gain life equal to the difference. Rhox Faithmender will then double that number. For example, if you have 3 life and an effect says that your life total \"becomes 10,\" your life total will actually become 17.")
    }
}
