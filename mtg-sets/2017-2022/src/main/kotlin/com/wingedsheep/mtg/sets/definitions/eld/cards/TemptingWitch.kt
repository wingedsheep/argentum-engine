package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tempting Witch
 * {2}{B}
 * Creature — Human Warlock
 * 1/3
 * When this creature enters, create a Food token.
 * {2}, {T}, Sacrifice a Food: Target player loses 3 life.
 *
 * "Sacrifice a Food" matches any Food artifact, not just tokens, so the cost filters on the Food
 * subtype as Bog Naughty does.
 */
val TemptingWitch = card("Tempting Witch") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{2}, {T}, Sacrifice a Food: Target player loses 3 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food")),
        )
        val player = target(Targets.Player)
        effect = Effects.LoseLife(3, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Mila Pesic"
        flavorText = "\"Murder clad in crimson beauty,\nAn end to life and love and duty.\"\n—Barrow witch incantation"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b217e5a-3c18-48b4-8124-b8c4fd7b2df1.jpg"
    }
}
