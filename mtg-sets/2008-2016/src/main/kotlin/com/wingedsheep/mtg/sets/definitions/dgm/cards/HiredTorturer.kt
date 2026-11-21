package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Hired Torturer — Dragon's Maze #25 (canonical printing; the card's only printing)
 * {2}{B} · Creature — Human Rogue · 2/3
 *
 * Defender
 * {3}{B}, {T}: Target opponent loses 2 life, then reveals a card at random from their hand.
 *
 * Prying Questions' life loss followed by the Wand of Ith / Friendly Fire random-reveal shape:
 * gather the opponent's hand, pick one card at random, reveal it. An empty hand reveals nothing,
 * so the opponent just loses 2 life (per the 2013-04-15 ruling).
 */
val HiredTorturer = card("Hired Torturer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 3
    oracleText = "Defender\n{3}{B}, {T}: Target opponent loses 2 life, then reveals a card at random from their hand."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.Tap)
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val revealed = chooseRandom(1, from = hand)
            reveal(revealed)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Winona Nelson"
        flavorText = "\"Follow your passion, I always say. Might as well get paid to do what you love.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62e9f79e-6606-4c9b-838c-eda5d8cc612c.jpg?1783940039"
        ruling("2013-04-15", "The card stops being revealed after Hired Torturer's ability resolves.")
        ruling(
            "2013-04-15",
            "You can choose an opponent with no cards in hand as the target of Hired Torturer's ability. " +
                "If the target opponent has no cards in hand when the ability resolves, that player will just lose 2 life."
        )
        ruling(
            "2013-04-15",
            "Each card in the opponent's hand has an equal chance of being revealed, even if it was previously " +
                "revealed because of Hired Torturer's ability."
        )
    }
}
