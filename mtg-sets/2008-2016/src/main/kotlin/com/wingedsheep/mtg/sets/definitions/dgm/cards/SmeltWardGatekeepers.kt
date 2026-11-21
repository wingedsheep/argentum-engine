package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Smelt-Ward Gatekeepers
 * {3}{R}
 * Creature — Human Warrior
 * 2/4
 * When this creature enters, if you control two or more Gates, gain control of target creature an
 * opponent controls until end of turn. Untap that creature. It gains haste until end of turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val SmeltWardGatekeepers = card("Smelt-Ward Gatekeepers") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, if you control two or more Gates, gain control of target " +
        "creature an opponent controls until end of turn. Untap that creature. It gains haste until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControlAtLeast(2, GameObjectFilter.Land.withSubtype("Gate"))
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8237b11f-36d2-4624-a0ef-520663385891.jpg?1783940037"
    }
}
