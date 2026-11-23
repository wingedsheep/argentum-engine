package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legion Loyalist
 * {R}
 * Creature — Goblin Soldier
 * 1/1
 * Haste
 * Battalion — Whenever this creature and at least two other creatures attack, creatures you control
 * gain first strike and trample until end of turn and can't be blocked by creature tokens this turn.
 *
 * The affected set is locked in on resolution (CR 611.2c), so `ForEachInGroup` snapshots the
 * creatures you control and grants each the keywords plus a floating `CantBeBlockedBy(token)` static.
 */
val LegionLoyalist = card("Legion Loyalist") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Soldier"
    power = 1
    toughness = 1
    oracleText = "Haste\nBattalion — Whenever this creature and at least two other creatures attack, " +
        "creatures you control gain first strike and trample until end of turn and can't be blocked by " +
        "creature tokens this turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity) then
                Effects.GrantStaticAbility(
                    ability = CantBeBlockedBy(GameObjectFilter.Creature.token()),
                    target = EffectTarget.IterationEntity,
                )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b47f639e-4635-4c26-bb2a-4925f0582c21.jpg?1783940123"
    }
}
