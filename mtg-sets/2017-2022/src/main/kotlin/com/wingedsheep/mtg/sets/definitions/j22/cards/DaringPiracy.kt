package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Daring Piracy — Jumpstart 2022 #33 (its only printing).
 * {2}{R} Enchantment
 *
 * At the beginning of combat on your turn, create a 1/1 red Pirate creature token with menace and
 * haste. Exile it at the beginning of the next end step.
 *
 * The delayed exile is `CreateToken(exileAtStep = Step.END)`, as on Lightning Coils. Scryfall has no
 * matching 1/1 red menace-haste Pirate token, so no `imageUri` is passed.
 */
val DaringPiracy = card("Daring Piracy") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, create a 1/1 red Pirate creature token with " +
        "menace and haste. Exile it at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Pirate"),
            keywords = setOf(Keyword.MENACE, Keyword.HASTE),
            exileAtStep = Step.END,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Manuel Castañón"
        flavorText = "Sky pirates often leap randomly into the fray and assume there will be something to land on."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec6ce176-a4f3-44dd-b66a-a23b6cf512e4.jpg?1783919183"
    }
}
