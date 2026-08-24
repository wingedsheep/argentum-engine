package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Soul Exchange
 * {B}{B}
 * Sorcery
 * As an additional cost to cast this spell, exile a creature you control.
 * Return target creature card from your graveyard to the battlefield. Put a +2/+2 counter on that
 * creature if the exiled creature was a Thrull.
 *
 * Miraculous Recovery's reanimation — the returned card keeps its entity id across the move, so
 * the counter lands on the same object — with the counter gated on what was exiled to cast it.
 * The additional cost is paid at cast time, so by resolution the exiled creature is a real card in
 * exile and its subtypes read straight off it.
 */
val SoulExchange = card("Soul Exchange") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, exile a creature you control.\n" +
        "Return target creature card from your graveyard to the battlefield. Put a +2/+2 counter " +
        "on that creature if the exiled creature was a Thrull."

    additionalCost(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Creature.youControl(),
            fromZone = CostZone.BATTLEFIELD
        )
    )

    spell {
        val creatureCard = target(
            "target creature card from your graveyard",
            Targets.CreatureCardInYourGraveyard
        )
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
            .then(
                ConditionalEffect(
                    condition = Conditions.ExiledAsCostHadSubtype("Thrull"),
                    effect = Effects.AddCounters(Counters.PLUS_TWO_PLUS_TWO, 1, creatureCard)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f73597d-f453-4d37-b2ef-c54ef683a884.jpg?1783947899"
    }
}
