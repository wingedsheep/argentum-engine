package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pulse of Murasa
 * {2}{G}
 * Instant
 *
 * Return target creature or land card from a graveyard to its owner's hand. You gain 6 life.
 *
 * Any graveyard's creature or land card moves to its owner's hand, then the caster gains 6 life.
 * With its only target gone the spell doesn't resolve, so no life is gained (2019-07-12 ruling).
 */
val PulseOfMurasa = card("Pulse of Murasa") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Return target creature or land card from a graveyard to its owner's hand. You gain 6 life."

    spell {
        val card = target(TargetFilter(GameObjectFilter.CreatureOrLand, zone = Zone.GRAVEYARD))
        effect = Effects.Move(card, Zone.HAND) then Effects.GainLife(6)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Matt Stewart"
        flavorText = "\"Little flower twirl and bloom, arise from this your rocky tomb. Little warrior slash and brawl, be born again to free us all.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0c8057f-b45b-4f67-90cd-c808b5e9cbfa.jpg?1783937899"
        ruling("2019-07-12", "If the target card is an illegal target by the time Pulse of Murasa tries to resolve, the spell doesn't resolve. You won't gain 6 life.")
    }
}
