package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Omen of the Sun
 * {2}{W}
 * Enchantment
 *
 * Flash
 * When this enchantment enters, create two 1/1 white Human Soldier creature tokens and you gain
 * 2 life.
 * {2}{W}, Sacrifice this enchantment: Scry 2.
 *
 * The white member of the Omen cycle: one entry trigger making both tokens in a single
 * [Effects.CreateToken] (`count = 2`) and then gaining the life.
 */
val OmenOfTheSun = card("Omen of the Sun") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, create two 1/1 white Human Soldier creature tokens and you gain 2 life.\n" +
        "{2}{W}, Sacrifice this enchantment: Scry 2."

    keywords(Keyword.FLASH)

    // When this enchantment enters, create two 1/1 white Human Soldier creature tokens and you gain 2 life.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1e76f0e3-9411-401d-ab38-9c3c64769483.jpg?1783931455"
        ) then Effects.GainLife(2)
    }

    // {2}{W}, Sacrifice this enchantment: Scry 2.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{W}"),
            Costs.SacrificeSelf
        )
        effect = Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Piotr Dura"
        flavorText = "\"My time will come, when every soul will bask in my glory and obey my law.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86586fe3-1a6e-4648-b3ff-b0d9340e66ff.jpg?1783931592"
    }
}
