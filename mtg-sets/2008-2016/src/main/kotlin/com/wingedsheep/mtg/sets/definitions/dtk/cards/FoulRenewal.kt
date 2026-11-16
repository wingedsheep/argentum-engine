package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Foul Renewal
 * {3}{B}
 * Instant
 *
 * Return target creature card from your graveyard to your hand. Target creature gets -X/-X until
 * end of turn, where X is the toughness of the card returned this way.
 *
 * Both targets are bound by name, so a graveyard target dropped by the resolution-time legality
 * check can't shift the creature target into its slot. The returned card is tracked
 * (`moveTracked`) rather than read off the target: X is the toughness of the card that actually
 * reached your hand, read there (its printed toughness — no battlefield effects apply to it). When
 * nothing was returned — the graveyard target became illegal — the `ifNotEmpty` gate leaves the
 * creature unaffected, as the ruling requires.
 */
val FoulRenewal = card("Foul Renewal") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to your hand. Target creature gets " +
        "-X/-X until end of turn, where X is the toughness of the card returned this way."

    spell {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        val creature = target(TargetFilter.Creature)

        effect = Effects.Pipeline {
            val returned = moveTracked(
                gather(CardSource.Entity(creatureCard), name = "foulRenewalCard"),
                CardDestination.ToZone(Zone.HAND)
            )
            ifNotEmpty(returned) {
                val shrink = -DynamicAmounts.toughnessOf(returned.asTarget)
                run(Effects.ModifyStats(shrink, shrink, creature))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Filip Burburan"
        flavorText = "The cycle of life and death is rarely pretty."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e319b7ca-d0a4-43ff-8e87-2af6ead089f5.jpg?1783938597"
        ruling(
            "2015-02-25",
            "You must target a creature card in your graveyard and a creature on the battlefield to " +
                "cast Foul Renewal. If the creature becomes an illegal target before Foul Renewal " +
                "resolves, you'll still return the creature card to your hand. If the creature card in " +
                "your graveyard becomes an illegal target, the creature on the battlefield will be " +
                "unaffected as there is no \"card returned this way.\""
        )
    }
}
