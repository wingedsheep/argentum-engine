package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Carrot Cake {1}{W}
 * Artifact — Food
 *
 * When this artifact enters and when you sacrifice it, create a 1/1 white
 * Rabbit creature token and scry 1.
 * {2}, {T}, Sacrifice this artifact: You gain 3 life.
 *
 * Note: "When you sacrifice it" is approximated using the Dies trigger
 * (battlefield → graveyard with SELF binding). This covers sacrifice cases
 * and also destroy, which is slightly broader than oracle text specifies.
 */
val CarrotCake = card("Carrot Cake") {
    manaCost = "{1}{W}"
    typeLine = "Artifact — Food"
    oracleText = "When this artifact enters and when you sacrifice it, create a 1/1 white Rabbit creature token and scry 1.\n{2}, {T}, Sacrifice this artifact: You gain 3 life."

    val createRabbitAndScry = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.WHITE),
        creatureTypes = setOf("Rabbit"),
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81de52ef-7515-4958-abea-fb8ebdcef93c.jpg?1721431122"
    ).then(EffectPatterns.scry(1))

    // When this artifact enters — create Rabbit token + scry 1
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = createRabbitAndScry
    }

    // When you sacrifice it — same effect (create Rabbit token + scry 1)
    triggeredAbility {
        trigger = Triggers.Dies
        effect = createRabbitAndScry
    }

    // Standard Food ability: {2}, {T}, Sacrifice: You gain 3 life
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Forrest Imel"
        flavorText = "\"Secret ingredient? What do you think?\"\n—Ms. Bumbleflower"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb03bb4f-8b4b-417e-bfc6-294cd2186b2e.jpg?1721425791"
        ruling("2024-07-26", "Carrot Cake's first ability will trigger whether you sacrifice it to pay the cost of its own last ability or due to another cost or effect.")
    }
}
