package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tower of Coireall
 * {2}
 * Artifact
 * {T}: Target creature can't be blocked by Walls this turn.
 *
 * Bog Rats' printed evasion, handed to someone else for a turn. `CantBeBlockedBy` is one of the
 * static abilities `Effects.GrantStaticAbility` can hand out and have take effect: the blocker
 * check reads granted restrictions keyed to the attacker alongside the printed ones.
 *
 * The filter is by subtype, not by the Defender keyword — the card says "Walls", and a Wall that
 * has somehow lost defender is still a Wall.
 */
val TowerOfCoireall = card("Tower of Coireall") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{T}: Target creature can't be blocked by Walls this turn."

    activatedAbility {
        cost = Costs.Tap
        target = Targets.Creature
        effect = Effects.GrantStaticAbility(
            ability = CantBeBlockedBy(GameObjectFilter.Creature.withSubtype(Subtype.WALL)),
            target = EffectTarget.ContextTarget(0),
            duration = Duration.EndOfTurn,
        )
        description = "{T}: Target creature can't be blocked by Walls this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64c19977-ac7d-4ce7-925c-33a7503420f5.jpg?1783947924"
    }
}
