package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nezumi Graverobber // Nighteyes the Desecrator (Champions of Kamigawa #129) — a flip card (CR 710).
 *
 * Nezumi Graverobber {1}{B} — Creature — Rat Rogue 2/1
 * "{1}{B}: Exile target card from an opponent's graveyard. If no cards are in that graveyard,
 * flip this creature."
 *
 * Nighteyes the Desecrator — Legendary Creature — Rat Wizard 4/2
 * "{4}{B}: Put target creature card from a graveyard onto the battlefield under your control."
 *
 * "That graveyard" is the graveyard of the exiled card's owner, checked after the exile. If the
 * target is gone on resolution the ability doesn't resolve, so there is no flip either.
 */
private val NezumiGraverobberUpright = card("Nezumi Graverobber") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Rogue"
    oracleText = "{1}{B}: Exile target card from an opponent's graveyard. If no cards are in that " +
        "graveyard, flip this creature."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        val t = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(t) then
            Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.count(Player.OwnerOf("target card"), Zone.GRAVEYARD),
                    ComparisonOperator.EQ,
                    0,
                ),
                Effects.Flip(),
            )
        description = "{1}{B}: Exile target card from an opponent's graveyard. If no cards are in " +
            "that graveyard, flip this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Jim Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77ffd913-8efa-48e5-a5cf-293d3068dbbf.jpg?1783944311"
    }
}

private val NighteyesTheDesecrator = card("Nighteyes the Desecrator") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Rat Wizard"
    oracleText = "{4}{B}: Put target creature card from a graveyard onto the battlefield under your control."
    power = 4
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        val t = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.PutOntoBattlefieldUnderYourControl(t)
        description = "{4}{B}: Put target creature card from a graveyard onto the battlefield under " +
            "your control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Jim Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77ffd913-8efa-48e5-a5cf-293d3068dbbf.jpg?1783944311"
    }
}

val NezumiGraverobber: CardDefinition = CardDefinition.flipCard(
    unflipped = NezumiGraverobberUpright,
    flipped = NighteyesTheDesecrator,
)
