package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MayPayXForEffect
import com.wingedsheep.sdk.core.Keyword

/**
 * Decree of Justice
 * {X}{X}{2}{W}{W}
 * Sorcery
 * Create X 4/4 white Angel creature tokens with flying.
 * Cycling {2}{W}
 * When you cycle Decree of Justice, you may pay {X}. If you do,
 * create X 1/1 white Soldier creature tokens.
 */
val DecreeOfJustice = card("Decree of Justice") {
    manaCost = "{X}{X}{2}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Create X 4/4 white Angel creature tokens with flying.\nCycling {2}{W}\nWhen you cycle Decree of Justice, you may pay {X}. If you do, create X 1/1 white Soldier creature tokens."

    spell {
        effect = CreateTokenEffect(
            count = DynamicAmount.XValue,
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}{W}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = MayPayXForEffect(
            effect = CreateTokenEffect(
                count = DynamicAmount.XValue,
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e8a7e5c-f252-4de8-94d7-e7327210bf26.jpg?1562529682"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
        ruling("2018-03-16", "A mana cost of {X}{X} means that you pay twice X. If you want X to be 3, you pay {8}{W}{W} to cast Decree of Justice.")
    }
}
