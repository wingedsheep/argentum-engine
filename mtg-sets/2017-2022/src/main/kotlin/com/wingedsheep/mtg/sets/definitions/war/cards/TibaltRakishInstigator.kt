package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tibalt, Rakish Instigator
 * {2}{R}
 * Legendary Planeswalker — Tibalt
 * Loyalty: 5
 *
 * Your opponents can't gain life.
 * −2: Create a 1/1 red Devil creature token with "When this token dies, it deals 1 damage to any
 * target."
 *
 * The static is a [PreventLifeGain] replacement on each opponent (as Archfiend of Despair). The
 * Devil carries its death trigger as a token-level ability (as Dance with Devils).
 */
val TibaltRakishInstigator = card("Tibalt, Rakish Instigator") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Tibalt"
    startingLoyalty = 5
    oracleText = "Your opponents can't gain life.\n" +
        "−2: Create a 1/1 red Devil creature token with \"When this token dies, it deals 1 damage to any target.\""

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent)))

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Devil"),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    val any = target(Targets.Any)
                    effect = Effects.DealDamage(1, any)
                    description = "When this token dies, it deals 1 damage to any target."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/7/2/72dc3d65-1729-4541-9dc3-bd673e9bdc5d.jpg?1783933349",
        )
        description = "Create a 1/1 red Devil creature token with \"When this token dies, it deals 1 damage to any target.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f9a24dc-8bc3-4528-8780-57fb108fdfbf.jpg?1783933421"
    }
}
