package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mechanozoa
 * {4}{U}{U}
 * Artifact Creature — Robot Jellyfish
 * When this creature enters, tap target artifact or creature an opponent controls and put a stun counter on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)
 * Warp {2}{U} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)
 * 5/5
 */
val Mechanozoa = card("Mechanozoa") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Jellyfish"
    oracleText = "When this creature enters, tap target artifact or creature an opponent controls and put a stun counter on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Warp {2}{U} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        val artifactOrCreatureOpponentControls = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).opponentControls()))
        effect = Effects.Tap(artifactOrCreatureOpponentControls) then
            Effects.AddCounters(CounterType.STUN, 1, artifactOrCreatureOpponentControls)
    }

    warp = "{2}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cb8d8ce-329a-4a97-b3d8-796703ebcb37.jpg?1752946818"
    }
}
