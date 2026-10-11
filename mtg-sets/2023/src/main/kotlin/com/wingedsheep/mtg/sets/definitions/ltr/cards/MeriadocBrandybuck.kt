package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Meriadoc Brandybuck
 * {1}{G}
 * Legendary Creature — Halfling Citizen
 * 2/2
 *
 * Whenever one or more Halflings you control attack a player, create a Food token.
 */
val MeriadocBrandybuck = card("Meriadoc Brandybuck") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Halfling Citizen"
    power = 2
    toughness = 2
    oracleText = "Whenever one or more Halflings you control attack a player, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    triggeredAbility {
        trigger = Triggers.you.attacksAPlayer(GameObjectFilter.Creature.youControl().withSubtype("Halfling"))
        effect = Effects.CreateFood(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Marie Magny"
        flavorText = "\"My dear old Hobbit, you don't allow for the inquisitiveness of friends. I have known about the existence of the Ring for years.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/885a3277-ef10-4dcf-ac63-eb8971cd627c.jpg?1686969480"
    }
}
