package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cut of the Profits
 * {X}{B}{B}
 * Sorcery
 * Casualty 3 (As you cast this spell, you may sacrifice a creature with power 3 or greater. When you do, copy this spell.)
 * You draw X cards and you lose X life.
 *
 * The casualty copy inherits the original's X (CR 707.10 — copiable cast-time choices), so with
 * casualty paid you draw 2X cards and lose 2X life across the two resolutions.
 */
val CutOfTheProfits = card("Cut of the Profits") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Casualty 3 (As you cast this spell, you may sacrifice a creature with power 3 or greater. When you do, copy this spell.)\nYou draw X cards and you lose X life."

    keywordAbility(KeywordAbility.casualty(3))

    spell {
        effect = Effects.DrawCards(DynamicAmounts.xValue()) then
            Effects.LoseLife(DynamicAmounts.xValue(), EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80e86078-1629-40dc-88a9-102c543255b8.jpg?1783923136"

        ruling("2022-04-29", "X will be the same for both the original spell and the copy you create with the casualty ability.")
        ruling("2022-04-29", "If you pay the casualty cost of a spell, the copy will resolve before the original spell.")
        ruling(
            "2022-04-29",
            "The copy of the spell is created on the stack, so it's not \"cast.\" Abilities that trigger " +
                "when a player casts a spell won't trigger."
        )
    }
}
