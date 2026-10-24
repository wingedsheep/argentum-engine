package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unwilling Ingredient
 * {B}
 * Creature — Frog
 * 1/1
 *
 * Menace
 * {2}{B}, Exile this card from your graveyard: You draw a card and you lose 1 life.
 */
val UnwillingIngredient = card("Unwilling Ingredient") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Frog"
    power = 1
    toughness = 1
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "{2}{B}, Exile this card from your graveyard: You draw a card and you lose 1 life."

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.ExileSelf)
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        activateFromZone = Zone.GRAVEYARD
        description = "{2}{B}, Exile this card from your graveyard: You draw a card and you lose 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "David Auden Nash"
        flavorText = "\"Don't look at me like that. It was either this or being eaten by a groff.\"\n" +
            "—Dina, Witherbloom mage-student"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30448144-639a-43c7-a408-bd6ed543c231.jpg?1783927360"
    }
}
