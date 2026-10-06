package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Caterpillar
 * {3}{G}
 * Creature — Insect
 * 3/3
 *
 * {G}, Sacrifice this creature: Create a 1/1 green Insect creature token with flying named
 * Butterfly at the beginning of the next end step.
 *
 * Same shape as Transluminant: the ability schedules a step-based delayed trigger for the next
 * beginning-of-end-step rather than creating the token on resolution. Oracle errata (2006) turned
 * the original "Butterfly token" into an Insect token named Butterfly.
 */
val GiantCaterpillar = card("Giant Caterpillar") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 3
    oracleText = "{G}, Sacrifice this creature: Create a 1/1 green Insect creature token with " +
        "flying named Butterfly at the beginning of the next end step."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.SacrificeSelf)
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Insect"),
                keywords = setOf(Keyword.FLYING),
                name = "Butterfly",
                imageUri = "https://cards.scryfall.io/normal/front/d/7/d7636957-f183-4f58-87a6-056cde657114.jpg?1783916646",
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Zina Saunders"
        flavorText = "\"'I've seen hornworms big as a man's fist,' the traveler said, and nodded " +
            "soberly when our jaws went slack at his ignorance.\"\n—Afari, Tales"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f602a6-3d35-49a3-b5cb-d754e03a9573.jpg?1783946983"
        ruling("2006-02-01", "It used to make Butterfly tokens. Now it makes Insect tokens named Butterfly.")
    }
}
