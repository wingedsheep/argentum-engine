package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ironhoof Boar — Kamigawa: Neon Dynasty #148 (canonical printing)
 * {5}{R} · Artifact Creature — Boar · 5/4
 *
 * Trample, haste
 * Channel — {1}{R}, Discard this card: Target creature gets +3/+1 and gains trample until end of turn.
 */
val IronhoofBoar = card("Ironhoof Boar") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Boar"
    power = 5
    toughness = 4
    oracleText = "Trample, haste\n" +
        "Channel — {1}{R}, Discard this card: Target creature gets +3/+1 and gains trample until end of turn."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    // Channel — {1}{R}, Discard this card (from hand): pump and grant trample to target creature.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 1, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        description = "Channel — {1}{R}, Discard this card: Target creature gets +3/+1 and gains " +
            "trample until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73abe574-6fb8-4809-9c18-0cf989f986f5.jpg?1783923866"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
    }
}
