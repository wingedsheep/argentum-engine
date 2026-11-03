package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Worf, Intelligence Contractor — Star Trek #180
 * {2}{R} · Legendary Creature — Klingon Warrior Spy · 3/2
 *
 * "For each opponent, … up to one target creature that player controls with mana value 3 or less"
 * is the corpus's one-per-opponent distribution (Tempted by the Oriq / In the Darkness Bind Them):
 * `dynamicMaxCount = PlayerCount(EachOpponent)` + `differentControllers = true`, `optional` for the
 * "up to". Each chosen creature is stolen until end of turn, untapped, and until end of turn becomes
 * a Warrior in addition to its other types and gains haste.
 */
val WorfIntelligenceContractor = card("Worf, Intelligence Contractor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Klingon Warrior Spy"
    power = 3
    toughness = 2
    oracleText = "When Worf enters, for each opponent, gain control of up to one target creature that " +
        "player controls with mana value 3 or less until end of turn. Untap those creatures. Until end " +
        "of turn, they become Warriors in addition to their other types and gain haste."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Creature.opponentControls().manaValueAtMost(3)),
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(
            Effects.GainControl(EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.AddCreatureType("Warrior", EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Jodie Muir"
        flavorText = "\"At the first sign of betrayal, I will kill him. But I promise to return the body intact.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7df6c8c1-1f85-4c5d-a4f6-9ffcdad1b12e.jpg?1792508250"
    }
}
