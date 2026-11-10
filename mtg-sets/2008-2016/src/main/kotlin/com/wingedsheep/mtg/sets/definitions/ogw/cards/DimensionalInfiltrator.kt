package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dimensional Infiltrator — Oath of the Gatewatch #44
 * {1}{U} · Creature — Eldrazi · Rare
 * 2/1
 *
 * Devoid (This card has no color.)
 * Flash
 * Flying
 * {1}{C}: Target opponent exiles the top card of their library. If it's a land card, you may
 * return this creature to its owner's hand. ({C} represents colorless mana.)
 *
 * Implementation notes:
 * - The top card goes to its owner's (the opponent's) exile, face up; the land check reads the
 *   card that actually moved, so an empty library exiles nothing and offers no return.
 * - The "may" is answered as the ability resolves (ruling). `sourceRequiredZone = BATTLEFIELD`
 *   skips the prompt when the Infiltrator has already left the battlefield — the ability still
 *   exiles the card, but there is no creature to return.
 */
val DimensionalInfiltrator = card("Dimensional Infiltrator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi"
    power = 2
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Flash\n" +
        "Flying\n" +
        "{1}{C}: Target opponent exiles the top card of their library. If it's a land card, you may " +
        "return this creature to its owner's hand. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID, Keyword.FLASH, Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{C}")
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, opponent.asPlayer))
            val exiled = moveTracked(top, CardDestination.ToZone(Zone.EXILE, opponent.asPlayer))
            run(
                Effects.If(
                    condition = whenMatches(exiled, GameObjectFilter.Land),
                    then = Effects.May(
                        Effects.ReturnToHand(EffectTarget.Self),
                        prompt = "Return Dimensional Infiltrator to its owner's hand?",
                        sourceRequiredZone = Zone.BATTLEFIELD
                    )
                )
            )
        }
        description = "{1}{C}: Target opponent exiles the top card of their library. If it's a land " +
            "card, you may return this creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ea28dd5-57b0-4255-a3d9-1c190c446f20.jpg?1783937921"
        ruling("2016-01-22", "You choose whether to return Dimensional Infiltrator to its owner's hand as its ability resolves. If the exiled card is a land card, you can't wait and return Dimensional Infiltrator at a later time.")
    }
}
