package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Yue, the Moon Spirit
 * {3}{U}
 * Legendary Creature — Spirit Ally
 * 3/3
 *
 * Flash
 * Flying, vigilance
 * Waterbend {5}, {T}: You may cast a noncreature spell from your hand without paying its mana cost.
 *
 * The waterbend cost is the existing activated-ability carrier (`hasWaterbend = true`). The body is
 * the standard gather → choose-up-to-one → cast-without-paying pipeline (as on Kellan, the Kid):
 * [GatherCardsEffect] pulls noncreature, nonland cards from hand, [SelectFromCollectionEffect]
 * makes the "you may" choice (up to one), and [Effects.CastFromCollectionWithoutPayingCost] casts
 * the chosen card for free.
 */
val YueTheMoonSpirit = card("Yue, the Moon Spirit") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Spirit Ally"
    power = 3
    toughness = 3
    oracleText = "Flash\n" +
        "Flying, vigilance\n" +
        "Waterbend {5}, {T}: You may cast a noncreature spell from your hand without paying its mana cost. " +
        "(While paying a waterbend cost, you can tap your artifacts and creatures to help. Each one pays for {1}.)"

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        hasWaterbend = true
        effect = Effects.Composite(
            GatherCardsEffect(
                CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.Nonland.notCreature()),
                storeAs = "yueCandidates",
            ),
            SelectFromCollectionEffect(
                from = "yueCandidates",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "yueChosen",
                selectedLabel = "Cast without paying its mana cost",
            ),
            Effects.CastFromCollectionWithoutPayingCost("yueChosen"),
        )
        description = "You may cast a noncreature spell from your hand without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Yuumei"
        flavorText = "\"You'll save the world again. But you can't give up.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecdac50f-c639-43fc-a03e-d488fac96ae2.jpg?1764120573"
    }
}
