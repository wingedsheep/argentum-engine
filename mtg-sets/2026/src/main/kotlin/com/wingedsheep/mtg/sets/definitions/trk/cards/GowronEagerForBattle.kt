package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gowron, Eager for Battle
 * {2}{B}{R}
 * Legendary Creature — Klingon Warrior
 * 3/3
 * Whenever another creature dies, put a +1/+1 counter on Gowron.
 * Sacrifice another creature: Gowron gains trample until end of turn. If you sacrificed a Klingon
 * this way, you draw a card and lose 1 life.
 *
 * The Klingon rider reads `SacrificedHadSubtype`, which checks the projected subtypes snapshotted
 * when the creature was sacrificed to pay the cost — its last-known types, not what it is in the
 * graveyard. The same shape as Ebon Praetor's "if the sacrificed creature was a Thrull".
 */
val GowronEagerForBattle = card("Gowron, Eager for Battle") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Klingon Warrior"
    oracleText = "Whenever another creature dies, put a +1/+1 counter on Gowron.\n" +
        "Sacrifice another creature: Gowron gains trample until end of turn. If you sacrificed a " +
        "Klingon this way, you draw a card and lose 1 life."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SacrificedHadSubtype("Klingon"),
                then = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
            )
        description = "Sacrifice another creature: Gowron gains trample until end of turn. If you " +
            "sacrificed a Klingon this way, you draw a card and lose 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Grant Griffin"
        flavorText = "\"If you lack the courage to fight an uphill battle, then perhaps you should retire to your bed.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d60613c2-5655-4051-babd-fc0f3a73fc77.jpg?1792508332"
    }
}
