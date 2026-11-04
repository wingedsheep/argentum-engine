package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tetzimoc, Primal Death
 * {4}{B}{B}
 * Legendary Creature — Elder Dinosaur
 * 6/6
 *
 * Deathtouch
 * {B}, Reveal this card from your hand: Put a prey counter on target creature. Activate only
 * during your turn.
 * When Tetzimoc enters, destroy each creature your opponents control with a prey counter on it.
 *
 * The middle ability works only from the hand ([Costs.RevealSelf] with `activateFromZone =
 * Zone.HAND`); the card stays there, so it can be activated any number of times on your turn
 * (ruling). The entry trigger reads every prey counter, whoever put it there (ruling).
 */
val TetzimocPrimalDeath = card("Tetzimoc, Primal Death") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Elder Dinosaur"
    power = 6
    toughness = 6
    oracleText = "Deathtouch\n" +
        "{B}, Reveal this card from your hand: Put a prey counter on target creature. " +
        "Activate only during your turn.\n" +
        "When Tetzimoc enters, destroy each creature your opponents control with a prey counter on it."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.RevealSelf)
        activateFromZone = Zone.HAND
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PREY, 1, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DestroyAll(
            GameObjectFilter.Creature.opponentControls().withCounter(CounterType.PREY)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6a0b40e-7a43-4722-b787-a5d7ec6453ac.jpg?1783935305"
    }
}
