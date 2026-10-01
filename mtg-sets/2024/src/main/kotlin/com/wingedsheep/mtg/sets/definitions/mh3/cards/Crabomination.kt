package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Crabomination — Modern Horizons 3 #85
 * {4}{B}{B} · Creature — Crab Demon · Rare
 * 5/5
 *
 * Emerge from artifact {5}{B}{B}
 * When this creature enters, target opponent exiles the top card of their library, a card at
 * random from their graveyard, and a card at random from their hand. You may cast a spell from
 * among cards exiled this way without paying its mana cost.
 *
 * Implementation notes:
 * - "Emerge from artifact" is the CR 702.119b variant: `emerge(cost, from = Artifact)`.
 * - The three exiled cards are one simultaneous exile. The pipeline has no union step, so the
 *   union is built from set differences over the opponent's library + graveyard + hand: strip
 *   the three picks from that pool to get "everything untouched", then subtract that from the
 *   pool again. The picks come from disjoint zones, so the result is exactly the three cards.
 * - The free cast happens during the trigger's resolution (ruling); a land can't be chosen.
 */
val Crabomination = card("Crabomination") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Crab Demon"
    power = 5
    toughness = 5
    oracleText = "Emerge from artifact {5}{B}{B} (You may cast this spell by sacrificing an artifact " +
        "and paying the emerge cost reduced by that artifact's mana value.)\n" +
        "When this creature enters, target opponent exiles the top card of their library, a card at " +
        "random from their graveyard, and a card at random from their hand. You may cast a spell " +
        "from among cards exiled this way without paying its mana cost."

    emerge("{5}{B}{B}", from = GameObjectFilter.Artifact)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val pool = gather(
                CardSource.FromMultipleZones(
                    listOf(Zone.LIBRARY, Zone.GRAVEYARD, Zone.HAND),
                    opponent.asPlayer
                )
            )
            val topCard = gather(CardSource.TopOfLibrary(1, opponent.asPlayer))
            val graveyardCard = chooseRandom(
                1,
                from = gather(CardSource.FromZone(Zone.GRAVEYARD, opponent.asPlayer))
            )
            val handCard = chooseRandom(
                1,
                from = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            )
            val untouched = exclude(exclude(exclude(pool, topCard), graveyardCard), handCard)
            val picked = exclude(pool, untouched)
            val exiled = moveTracked(picked, CardDestination.ToZone(Zone.EXILE))
            val spell = chooseUpTo(
                1,
                from = exiled,
                filter = GameObjectFilter.Nonland,
                showAllCards = true,
                prompt = "You may cast a spell from among the exiled cards without paying its mana cost",
                selectedLabel = "Cast for free"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(spell))
        }
        description = "When this creature enters, target opponent exiles the top card of their " +
            "library, a card at random from their graveyard, and a card at random from their hand. " +
            "You may cast a spell from among cards exiled this way without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f328d6e0-d808-4abe-b6a2-cc557b27c329.jpg?1783911283"
        ruling("2024-06-07", "Emerge from artifact is a variant of the emerge ability. It allows you to sacrifice an artifact rather than a creature, but otherwise functions identically to emerge.")
        ruling("2024-06-07", "You may sacrifice an artifact with a mana value of 0, such as a Food token, to cast Crabomination for its emerge cost. You'll just pay the full emerge cost with no reduction.")
        ruling("2024-06-07", "You may sacrifice an artifact with mana value greater than or equal to the emerge cost. If you do, you'll pay only the colored mana component of the emerge cost.")
        ruling("2024-06-07", "The mana value of a creature spell with emerge isn't affected by whether its emerge cost is paid. For example, if you cast Crabomination for its emerge cost and sacrifice an artifact whose mana value is 3, Crabomination's mana value remains 6.")
        ruling("2024-06-07", "Crabomination's triggered ability doesn't allow you to play land cards, and as such, any land cards exiled with Crabomination's triggered ability will remain in exile. If you choose not to cast any of the exiled nonland cards (either because you can't or don't want to), those cards will remain in exile as well.")
        ruling("2024-06-07", "You choose whether or not to cast one of the exiled cards as Crabomination's triggered ability resolves. If you do, you do so as part of the resolution of that ability. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2024-06-07", "If you cast a spell for another cost \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, those must be paid to cast it.")
    }
}
