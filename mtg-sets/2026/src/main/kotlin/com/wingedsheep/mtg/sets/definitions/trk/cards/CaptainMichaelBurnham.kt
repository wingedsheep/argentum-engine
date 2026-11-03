package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captain Michael Burnham
 * {2}{R}
 * Legendary Creature — Human Officer
 * 3/3
 *
 * Trample
 * Landfall — Whenever a land you control enters, Captain Burnham deals 1 damage to each
 * opponent. If this is the second time this ability has resolved this turn, put a +1/+1
 * counter on Captain Burnham.
 */
val CaptainMichaelBurnham = card("Captain Michael Burnham") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Officer"
    power = 3
    toughness = 3
    oracleText = "Trample\nLandfall — Whenever a land you control enters, Captain Burnham deals 1 damage to each opponent. If this is the second time this ability has resolved this turn, put a +1/+1 counter on Captain Burnham."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Romana Kendelic"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e10d6d71-9360-4ecf-97c8-22afb22aaeb7.jpg?1792508221"
    }
}
