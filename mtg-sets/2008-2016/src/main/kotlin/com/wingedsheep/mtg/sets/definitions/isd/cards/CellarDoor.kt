package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Cellar Door — Innistrad #218
 * {2} · Artifact · Uncommon
 *
 * {3}, {T}: Target player puts the bottom card of their library into their graveyard. If it's a
 * creature card, you create a 2/2 black Zombie creature token.
 *
 * Not a mill — the card comes off the *bottom* ([CardSource.BottomOfLibrary]). The creature check
 * reads the gathered card, so "it" is the card that was moved, not whatever is in the graveyard.
 */
val CellarDoor = card("Cellar Door") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Target player puts the bottom card of their library into their graveyard. " +
        "If it's a creature card, you create a 2/2 black Zombie creature token."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val bottomCard = gather(CardSource.BottomOfLibrary(1, player.asPlayer))
            toGraveyard(bottomCard, player.asPlayer)
            run(Effects.If(
                condition = Conditions.CollectionContainsMatch(bottomCard, GameObjectFilter.Creature),
                then = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                ),
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Rob Alexander"
        flavorText = "As if anyone needed another reason to avoid dark, dank cellars."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97bdfb00-7773-4af6-895c-c90088a96b07.jpg"
    }
}
