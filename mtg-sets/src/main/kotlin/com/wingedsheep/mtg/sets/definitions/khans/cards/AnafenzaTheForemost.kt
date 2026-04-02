package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Anafenza, the Foremost
 * {W}{B}{G}
 * Legendary Creature — Human Soldier
 * 4/4
 * Whenever Anafenza, the Foremost attacks, put a +1/+1 counter on another target tapped creature you control.
 * If a nontoken creature an opponent owns would die or a creature card not on the battlefield would be put
 * into an opponent's graveyard, exile that card instead.
 */
val AnafenzaTheForemost = card("Anafenza, the Foremost") {
    manaCost = "{W}{B}{G}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Whenever Anafenza, the Foremost attacks, put a +1/+1 counter on another target tapped creature you control.\nIf a nontoken creature an opponent owns would die or a creature card not on the battlefield would be put into an opponent's graveyard, exile that card instead."

    triggeredAbility {
        trigger = Triggers.Attacks
        val tappedCreature = target(
            "another target tapped creature you control",
            TargetCreature(filter = TargetFilter.TappedCreature.youControl().other())
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, tappedCreature)
    }

    // Exile nontoken creature cards owned by opponents that would go to graveyard
    // This covers both dying (battlefield → graveyard) and other zone → graveyard
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent(
                filter = GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsNontoken),
                    controllerPredicate = com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.OwnedByOpponent
                ),
                to = Zone.GRAVEYARD
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "163"
        artist = "James Ryman"
        flavorText = "Rarely at rest on the Amber Throne, Anafenza always leads the Abzan Houses to battle."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8b432a7-53da-4480-b571-e6feb1364a3a.jpg?1562793427"
    }
}
