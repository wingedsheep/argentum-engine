package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion

/**
 * Aphemia, the Cacophony
 * {1}{B}
 * Legendary Enchantment Creature — Harpy
 * 2/1
 * Flying
 * At the beginning of your end step, you may exile an enchantment card from your graveyard. If
 * you do, create a 2/2 black Zombie creature token.
 *
 * Masked Vandal's "you may exile a card from your graveyard. If you do, …" shape: [Effects.May]
 * around an [Effects.IfYouDo] whose action is a one-card gather → choose → exile pipeline, gated
 * on the choice being non-empty. The pick is made on resolution, so Aphemia itself can be the
 * card exiled if it died in response (ruling); exactly one card, so one Zombie at most.
 */
val AphemiaTheCacophony = card("Aphemia, the Cacophony") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment Creature — Harpy"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "At the beginning of your end step, you may exile an enchantment card from your graveyard. " +
        "If you do, create a 2/2 black Zombie creature token."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val enchantments = gather(
                        CardSource.FromZone(zone = Zone.GRAVEYARD, filter = GameObjectFilter.Enchantment)
                    )
                    // Named: the "if you do" check below reads it from outside the pipeline.
                    val toExile = chooseExactly(1, from = enchantments, selectedLabel = "Exile", name = "toExile")
                    exile(toExile)
                },
                then = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    imageUri = "https://cards.scryfall.io/normal/front/6/b/6b3e6b4d-d798-4c6a-8a09-255be2a6df2c.jpg?1783931454"
                ),
                successCriterion = SuccessCriterion.CollectionNonEmpty("toExile", min = 1)
            )
        )
        description = "At the beginning of your end step, you may exile an enchantment card from your " +
            "graveyard. If you do, create a 2/2 black Zombie creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Lucas Graciano"
        flavorText = "\"Sing, beauty. Sing for death, and for the dead.\"\n—Ashiok"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b359607-d699-4ba9-a438-48f0dd8f1bf5.jpg?1783931572"
        ruling(
            "2020-01-24",
            "While resolving Aphemia's triggered ability, you can't exile more than one enchantment card " +
                "to get more than one Zombie token."
        )
        ruling(
            "2020-01-24",
            "You choose whether to exile an enchantment card and which one to exile while Aphemia's " +
                "triggered ability is resolving. If Aphemia dies in response to its own triggered ability, " +
                "it could be the card you exile."
        )
    }
}
