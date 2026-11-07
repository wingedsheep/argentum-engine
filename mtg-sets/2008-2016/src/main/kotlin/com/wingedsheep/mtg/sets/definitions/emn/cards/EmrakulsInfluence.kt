package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Emrakul's Influence (Eldritch Moon #157)
 * {2}{G}{G}
 * Enchantment
 *
 * Whenever you cast an Eldrazi creature spell with mana value 7 or greater, draw two cards.
 */
val EmrakulsInfluence = card("Emrakul's Influence") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an Eldrazi creature spell with mana value 7 or greater, draw two cards."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withSubtype("Eldrazi").manaValueAtLeast(7))
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Ryan Alexander Lee"
        flavorText = "Hordes of Eldrazi and cultists swarmed over Thraben's defenses, and the members " +
            "of the Gatewatch found themselves overrun."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/112476e7-72ab-4ebc-9e6e-e96fbf5109c0.jpg"
    }
}
