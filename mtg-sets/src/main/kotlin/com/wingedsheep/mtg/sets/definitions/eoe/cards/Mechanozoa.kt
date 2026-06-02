package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter

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
        trigger = Triggers.EntersBattlefield
        target("target artifact or creature an opponent controls", TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls())))
        effect = Effects.Composite(
            listOf(
                Effects.Tap(EffectTarget.ContextTarget(0)),
                Effects.AddCounters(Counters.STUN, 1, EffectTarget.ContextTarget(0))
            )
        )
    }

    warp = "{2}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cb8d8ce-329a-4a97-b3d8-796703ebcb37.jpg?1752946818"
    }
}
