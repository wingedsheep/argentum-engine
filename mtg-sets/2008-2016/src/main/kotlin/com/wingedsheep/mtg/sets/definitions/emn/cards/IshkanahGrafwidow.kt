package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ishkanah, Grafwidow (Eldritch Moon #162)
 * {4}{G}
 * Legendary Creature — Spider
 * 3/5
 *
 * Reach
 * Delirium — When Ishkanah enters, if there are four or more card types among cards in your
 * graveyard, create three 1/2 green Spider creature tokens with reach.
 * {6}{B}: Target opponent loses 1 life for each Spider you control.
 *
 * The Spider count is a dynamic amount read as the activated ability resolves, so it includes
 * Ishkanah itself if it is still on the battlefield (per the 2016-07-13 ruling).
 */
val IshkanahGrafwidow = card("Ishkanah, Grafwidow") {
    manaCost = "{4}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Spider"
    power = 3
    toughness = 5
    oracleText = "Reach\n" +
        "Delirium — When Ishkanah enters, if there are four or more card types among cards in your " +
        "graveyard, create three 1/2 green Spider creature tokens with reach.\n" +
        "{6}{B}: Target opponent loses 1 life for each Spider you control."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Delirium(4)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH),
            count = 3,
            imageUri = "https://cards.scryfall.io/normal/front/c/e/ce90c48f-74fb-4e87-9e46-7f8c3d79cbb0.jpg?1783937414",
        )
        description = "Delirium — When Ishkanah enters, if there are four or more card types among " +
            "cards in your graveyard, create three 1/2 green Spider creature tokens with reach."
    }

    activatedAbility {
        cost = Costs.Mana("{6}{B}")
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withSubtype(Subtype.SPIDER),
            ).count(),
            opponent,
        )
        description = "{6}{B}: Target opponent loses 1 life for each Spider you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "162"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a6c5a98-2fe5-4671-a6a8-98a47c09c5a7.jpg?1783937444"
        ruling(
            "2016-07-13",
            "The number of Spiders you control is checked only as Ishkanah's last ability resolves. " +
                "The ability will count Ishkanah itself if it's still on the battlefield."
        )
    }
}
