package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rotfeaster Maggot
 * {4}{B}
 * Creature — Insect
 * 3/5
 * When this creature enters, exile target creature card from a graveyard. You gain life equal to
 * that card's toughness.
 *
 * The Soul Separator shape: exile first, then read [DynamicAmounts.toughnessOf] off the same
 * target. A target reference reads the live card, never a battlefield snapshot, so the value is
 * the card's own toughness in exile — a creature that died with +1/+1 counters yields its printed
 * toughness. The ability is mandatory (ruling), and with its only target gone by resolution it
 * fizzles, so no life is gained.
 */
val RotfeasterMaggot = card("Rotfeaster Maggot") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    oracleText = "When this creature enters, exile target creature card from a graveyard. " +
        "You gain life equal to that card's toughness."
    power = 3
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        val graveyardCreature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Exile(graveyardCreature) then
            Effects.GainLife(DynamicAmounts.toughnessOf(graveyardCreature))
        description = "When this creature enters, exile target creature card from a graveyard. " +
            "You gain life equal to that card's toughness."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Yeong-Hao Han"
        flavorText = "Is it at the top of the food chain or the bottom?"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c8e2442-0677-4ea6-8d70-4c39fbb552ed.jpg?1783939180"
        ruling("2014-07-18", "Rotfeaster Maggot's ability is mandatory. If you are the only player with creature cards in your graveyard, you must target one of them.")
    }
}
