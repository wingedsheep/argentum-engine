package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goremand
 * {4}{B}{B}
 * Creature — Demon
 * 5/5
 * As an additional cost to cast this spell, sacrifice a creature.
 * Flying
 * Trample
 * When this creature enters, each opponent sacrifices a creature.
 */
val Goremand = card("Goremand") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Flying\n" +
        "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "When this creature enters, each opponent sacrifices a creature."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "When this creature enters, each opponent sacrifices a creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9cd537c-e40e-438f-a751-e0ad8f6e6283.jpg?1783930707"
        ruling("2020-06-23", "As Goremand's triggered ability resolves, first the next opponent in turn order (or, if it's an opponent's turn, the opponent whose turn it is) chooses a creature they control, then each other opponent in turn order does the same knowing the choices made before them. Then all the chosen creatures are sacrificed at the same time.")
    }
}
