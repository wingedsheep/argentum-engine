package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gríma Wormtongue
 * {2}{B}
 * Legendary Creature — Human Advisor
 * 1/4
 *
 * Your opponents can't gain life.
 * {T}, Sacrifice another creature: Target player loses 1 life. If the sacrificed
 * creature was legendary, amass Orcs 2.
 *
 * The static is wired as a `PreventLifeGain` replacement applied to each opponent of the
 * source's controller — same primitive Sulfuric Vortex / Sunspine Lynx use, but scoped via
 * `Player.EachOpponent` so the source's controller is unaffected.
 *
 * The activated ability uses the new `SacrificedWasLegendary` condition (LTR Gap 17) to
 * append the amass rider when the sacrificed permanent's projected supertypes at the
 * moment of payment contained `LEGENDARY`.
 */
val GrimaWormtongue = card("Gríma Wormtongue") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 4
    oracleText = "Your opponents can't gain life.\n" +
        "{T}, Sacrifice another creature: Target player loses 1 life. " +
        "If the sacrificed creature was legendary, amass Orcs 2."

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent)))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        val player = target("target player", Targets.Player)
        effect = Effects.LoseLife(1, player)
            .then(ConditionalEffect(
                condition = Conditions.SacrificedWasLegendary,
                effect = Effects.Amass(2, "Orc")
            ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        flavorText = "\"Do not weary yourself, or tax too heavily your strength. Let others deal with these irksome guests.\""
        artist = "Alex Brock"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e3bd86b-e8ca-4885-a823-78fb967e6caf.jpg?1694569791"
    }
}
