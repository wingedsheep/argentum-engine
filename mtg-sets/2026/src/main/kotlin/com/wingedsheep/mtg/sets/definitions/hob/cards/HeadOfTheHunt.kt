package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.ReplacementRiderTiming

/**
 * Head of the Hunt — The Hobbit #75
 * {2}{B}{B}
 * Creature — Wolf
 * 4/3
 *
 * Flash
 * If a creature an opponent controls would die, exile it instead. When you do, create a 2/2 green
 * Wolf creature token.
 *
 * The whole clause is one replacement effect with a rider ([RedirectZoneChangeWith], The
 * Darkness Crystal) rather than a redirect plus a separate trigger: "when you do" is a reflexive
 * trigger of the replacement ([ReplacementRiderTiming.WHEN_YOU_DO], CR 603.12), so one Wolf trigger
 * goes on the stack per creature actually redirected. A standalone dies
 * trigger would never fire — the redirect means those creatures never die at all, and neither do
 * anyone else's dies triggers (CR 614.1c).
 *
 * The filter is deliberately *not* `nontoken()`: an opponent's token still "would die", so it is
 * exiled instead (and then ceases to exist as a state-based action) and still pays off the Wolf.
 */
val HeadOfTheHunt = card("Head of the Hunt") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wolf"
    power = 4
    toughness = 3
    oracleText = "Flash\n" +
        "If a creature an opponent controls would die, exile it instead. When you do, create a " +
        "2/2 green Wolf creature token."

    keywords(Keyword.FLASH)

    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.EXILE,
            additionalEffect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Wolf"),
            ),
            riderTiming = ReplacementRiderTiming.WHEN_YOU_DO,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Andrea Piparo"
        flavorText = "The Wargs planned to come by night upon the villages nearest the mountains. " +
            "If their plan had been carried out, there would have been none left there next day."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3ffe34d4-72f4-4562-a948-8909b9321e59.jpg?1785152416"
    }
}
