package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ray Fillet, Man Ray
 * {3}{U}
 * Legendary Creature — Fish Mutant
 * 3/3
 *
 * Flying
 * When Ray Fillet enters, create a Mutagen token.
 * {2}, Remove a +1/+1 counter from a creature you control: Draw a card.
 */
val RayFilletManRay = card("Ray Fillet, Man Ray") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Fish Mutant"
    oracleText = "Flying\nWhen Ray Fillet enters, create a Mutagen token. (It's an artifact with \"{1}, {T}, Sacrifice this token: Put a +1/+1 counter on target creature. Activate only as a sorcery.\")\n{2}, Remove a +1/+1 counter from a creature you control: Draw a card."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateMutagenToken()
        description = "When Ray Fillet enters, create a Mutagen token."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.RemovePlusOnePlusOneCounters(GameObjectFilter.Creature.youControl(), 1)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14e0892c-a556-43c3-974f-6be44188da2e.jpg?1771502589"
    }
}
