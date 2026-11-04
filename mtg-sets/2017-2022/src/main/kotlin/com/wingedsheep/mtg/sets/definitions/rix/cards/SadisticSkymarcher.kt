package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sadistic Skymarcher — Rivals of Ixalan #85
 * {2}{B} · Creature — Vampire Soldier · 2/2
 *
 * As an additional cost to cast this spell, reveal a Vampire card from your hand or pay {1}.
 * Flying, lifelink
 *
 * The reveal-or-pay cost is the Silvergill Adept shape: any Vampire *card*, not only a creature card.
 */
val SadisticSkymarcher = card("Sadistic Skymarcher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 2
    oracleText = "As an additional cost to cast this spell, reveal a Vampire card from your hand or pay {1}.\n" +
        "Flying, lifelink"

    additionalCost(
        Costs.additional.RevealFromHandOrPay(
            filter = GameObjectFilter.Any.withSubtype(Subtype.VAMPIRE),
            alternativeManaCost = "{1}"
        )
    )

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Seb McKinnon"
        flavorText = "\"I will catch you, child of the sun. I will drink your life and leave you a husk, " +
            "discarded in the shadows.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1334ee90-c798-42ec-b2be-68dac766a7eb.jpg?1783935307"
        ruling(
            "2018-01-19",
            "If an effect refers to a \"[subtype] spell\" or \"[subtype] card,\" it refers only to a spell or card " +
                "that has that subtype."
        )
    }
}
