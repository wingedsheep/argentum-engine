package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gilded Goose
 * {G}
 * Creature — Bird
 * 0/2
 * Flying
 * When this creature enters, create a Food token. (It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 * {1}{G}, {T}: Create a Food token.
 * {T}, Sacrifice a Food: Add one mana of any color.
 *
 * "Sacrifice a Food" matches any Food artifact, not just Food tokens (2024-11-08 ruling).
 */
val GildedGoose = card("Gilded Goose") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bird"
    power = 0
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n{1}{G}, {T}: Create a Food token.\n{T}, Sacrifice a Food: Add one mana of any color."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
        effect = Effects.CreateFood()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food")))
        manaAbility = true
        effect = Effects.AddAnyColorMana()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Lindsey Look"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30377bf0-d9b1-4c14-8dde-f74b1e02d604.jpg?1783932611"
        ruling("2024-11-08", "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token. For example, you can sacrifice Tough Cookie (an Artifact Creature — Food Golem) to activate Maraleaf Rider's ability (an ability with \"Sacrifice a Food\" in its cost).")
        ruling("2024-11-08", "You can't sacrifice a Food to pay multiple costs. For example, you can't sacrifice a Food token to activate its own ability and also to activate Maraleaf Rider's ability.")
        ruling("2024-11-08", "Food is an artifact type. Even though it appears on some creatures, it's never a creature type.")
    }
}
