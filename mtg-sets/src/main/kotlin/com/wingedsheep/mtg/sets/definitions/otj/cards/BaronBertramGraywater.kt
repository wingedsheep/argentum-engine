package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Baron Bertram Graywater
 * {2}{W}{B}
 * Legendary Creature — Vampire Noble
 * 3/4
 *
 * Whenever one or more tokens you control enter, create a 1/1 black Vampire Rogue creature token
 * with lifelink. This ability triggers only once each turn.
 * {1}{B}, Sacrifice another creature or artifact: Draw a card.
 *
 * - The first ability is the batched you-scoped token-ETB trigger
 *   ([Triggers.OneOrMorePermanentsEnter] on [GameObjectFilter.Token], which defaults to "you
 *   control") with `oncePerTurn = true` for "This ability triggers only once each turn"
 *   (CR 603.3 / engine-tracked once-per-turn). It fires once per batch, no matter how many tokens
 *   entered.
 * - The activated ability composes a mana + sacrifice-another cost over the
 *   [GameObjectFilter.CreatureOrArtifact] filter ("creature or artifact", excluding Baron himself)
 *   and draws a card.
 */
val BaronBertramGraywater = card("Baron Bertram Graywater") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Vampire Noble"
    power = 3
    toughness = 4
    oracleText = "Whenever one or more tokens you control enter, create a 1/1 black Vampire Rogue " +
        "creature token with lifelink. This ability triggers only once each turn.\n" +
        "{1}{B}, Sacrifice another creature or artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Token)
        oncePerTurn = true
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(1),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Vampire", "Rogue"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/5/5/55a35e07-9d80-4f2b-a000-5638b72a9808.jpg?1712316236"
        )
        description = "Whenever one or more tokens you control enter, create a 1/1 black Vampire " +
            "Rogue creature token with lifelink. This ability triggers only once each turn."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        )
        effect = Effects.DrawCards(1)
        description = "{1}{B}, Sacrifice another creature or artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Johan Grenier"
        flavorText = "\"Of course the law's on my side. I wrote it.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9da18b4-1efc-44b7-8001-a2cfd44c69bf.jpg?1712356055"

        ruling("2024-04-12", "If Baron Bertram Graywater enters under your control and is itself a token, its own ability will trigger and you'll create a Vampire Rogue token. If you also control a nontoken Baron Bertram Graywater, that one's ability will also trigger, netting you another Vampire Rogue.")
    }
}
