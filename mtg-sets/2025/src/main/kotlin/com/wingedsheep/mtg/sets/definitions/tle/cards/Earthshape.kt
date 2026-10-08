package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Earthshape
 * {2}{W}
 * Instant
 *
 * Earthbend 3. Then each creature you control with power less than or equal to that land's power
 * gains hexproof and indestructible until end of turn. You gain hexproof until end of turn.
 *
 * Earthbend is the set's composed keyword action (`Effects.Earthbend(3, land)`, CR 701.66a).
 * "That land's power" is read *after* the earthbend, so it sees the 0/0 base plus the three new
 * counters (and any counters already on it — the rulings allow re-earthbending a land creature).
 * The group is fixed as the step resolves (CR 611.2c — the Heroic Intervention shape over
 * [GroupFilter]), compared through `powerAtMostEntity(land)`, which reads both sides' projected
 * power; the land itself qualifies, so it is protected too. "You gain hexproof" is the player form
 * of [Effects.GrantHexproof].
 */
val Earthshape = card("Earthshape") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Earthbend 3. Then each creature you control with power less than or equal to that " +
        "land's power gains hexproof and indestructible until end of turn. You gain hexproof until end of turn."

    spell {
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(3, land) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl().powerAtMostEntity(land)),
                Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity) then
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
            ) then
            Effects.GrantHexproof(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Fahmi Fauzi"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d873c678-c8f2-40f0-ab03-a2fdf5187aea.jpg?1783904839"
        ruling("2025-10-02", "If the targeted land becomes an illegal target before the spell or ability that includes earthbend resolves, earthbend does nothing. If the spell or ability didn't have other targets, it won't resolve.")
        ruling("2025-10-02", "You may target a land that is already a creature, perhaps because of a previous earthbend ability. The land will get the +1/+1 counters, gain haste, and have its base power and toughness set to 0/0.")
    }
}
