package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Giant Shark
 * {5}{U}
 * Creature — Shark
 * 4/4
 * This creature can't attack unless defending player controls an Island.
 * Whenever this creature blocks or becomes blocked by a creature that has been dealt damage this
 * turn, this creature gets +2/+0 and gains trample until end of turn.
 * When you control no Islands, sacrifice this creature.
 *
 * Island Fish Jasconius' two water-bound clauses — the attack restriction and the state trigger —
 * plus a blood-in-the-water bonus. The last is the only new part: `BlocksOrBecomesBlockedBy` with a
 * `wasDealtDamageThisTurn` partner filter, so the Shark reacts to a creature already wounded this
 * turn rather than to one it is merely fighting.
 *
 * "Has been dealt damage this turn" is the passive voice: damage marked on the partner, whoever
 * dealt it, and it survives the damage being removed at cleanup within the same turn.
 *
 * The Island clause is a *state* trigger (CR 603.8), not a leaves-the-battlefield one: it fires
 * the moment the condition becomes true, including as your last Island is destroyed.
 */
val GiantShark = card("Giant Shark") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shark"
    power = 4
    toughness = 4
    oracleText = "This creature can't attack unless defending player controls an Island.\n" +
        "Whenever this creature blocks or becomes blocked by a creature that has been dealt " +
        "damage this turn, this creature gets +2/+0 and gains trample until end of turn.\n" +
        "When you control no Islands, sacrifice this creature."

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType("Island"))
    }

    triggeredAbility {
        trigger = Triggers.BlocksOrBecomesBlockedBy(
            GameObjectFilter.Creature.wasDealtDamageThisTurn()
        )
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
        )
        description = "Whenever this creature blocks or becomes blocked by a creature that has " +
            "been dealt damage this turn, this creature gets +2/+0 and gains trample until end of turn."
    }

    stateTriggeredAbility {
        condition = Conditions.YouControl(
            GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
            negate = true,
        )
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control no Islands, sacrifice this creature"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53ec4a19-0f2f-4713-a869-58832484648d.jpg?1783947942"
    }
}
