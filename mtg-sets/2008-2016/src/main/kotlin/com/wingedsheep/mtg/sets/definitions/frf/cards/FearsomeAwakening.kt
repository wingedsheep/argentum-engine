package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fearsome Awakening
 * {4}{B}
 * Sorcery
 *
 * Return target creature card from your graveyard to the battlefield. If it's a Dragon, put two
 * +1/+1 counters on it.
 *
 * Miraculous Recovery's reanimation (the returned card keeps its entity id across the move), with
 * the counters gated on the creature being a Dragon. The check runs *after* the move, as the ruling
 * says, so it reads the permanent's projected subtypes on the battlefield.
 */
val FearsomeAwakening = card("Fearsome Awakening") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield. " +
        "If it's a Dragon, put two +1/+1 counters on it."

    spell {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(
                    GameObjectFilter.Creature.withSubtype(Subtype.DRAGON),
                    creatureCard
                ),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creatureCard)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Véronique Meignaud"
        flavorText = "On Tarkir, dragons emerge from enormous tempests, phenomena tied to Ugin's very presence on the plane."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a672979-024a-4f69-91ae-a2cf4055e0a8.jpg?1783938697"
        ruling("2014-11-24", "No player can cast spells or activate abilities between returning the creature card to the battlefield and checking whether it's a Dragon.")
    }
}
