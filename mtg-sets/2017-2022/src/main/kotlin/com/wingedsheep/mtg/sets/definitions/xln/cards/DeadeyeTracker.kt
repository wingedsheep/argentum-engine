package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deadeye Tracker
 * {B}
 * Creature — Human Pirate
 * 1/1
 * {1}{B}, {T}: Exile two target cards from an opponent's graveyard. This creature explores.
 */
val DeadeyeTracker = card("Deadeye Tracker") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 1
    toughness = 1
    oracleText = "{1}{B}, {T}: Exile two target cards from an opponent's graveyard. This creature explores. " +
        "(Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a " +
        "+1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        targets(TargetFilter.CardInGraveyard.ownedByOpponent(), count = 2, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
        ) then Effects.Explore(EffectTarget.Self)
        description = "Exile two target cards from an opponent's graveyard, then explore"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/848902db-7a10-4754-b50a-57a9e845705a.jpg?1783935762"
        ruling(
            "2017-09-29",
            "If one target card is an illegal target by the time Deadeye Tracker's ability resolves, the " +
                "remaining legal target is exiled and Deadeye Tracker explores.",
        )
        ruling(
            "2017-09-29",
            "If each target card is an illegal target by the time Deadeye Tracker's ability resolves, the " +
                "entire ability doesn't resolve. Deadeye Tracker won't explore.",
        )
        ruling(
            "2017-09-29",
            "You can't activate Deadeye Tracker's ability without targeting two cards in a single " +
                "opponent's graveyard.",
        )
    }
}
