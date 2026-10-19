package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thundering Raiju — Kamigawa: Neon Dynasty #166 (canonical printing)
 * {2}{R}{R} · Creature — Spirit · 3/3
 *
 * Haste. Whenever this creature attacks, put a +1/+1 counter on target creature you control. Then
 * this creature deals X damage to each opponent, where X is the number of modified creatures you
 * control other than this creature.
 *
 * The counter lands first, so X (read on resolution) counts the creature that just got it — unless
 * that creature is the Raiju itself, which `excludeSelf` leaves out. "Modified" is
 * [StatePredicate.IsModified] (CR 700.9).
 */
val ThunderingRaiju = card("Thundering Raiju") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Whenever this creature attacks, put a +1/+1 counter on target creature you control. Then this " +
        "creature deals X damage to each opponent, where X is the number of modified creatures you " +
        "control other than this creature. (Equipment, Auras you control, and counters are modifications.)"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.DealDamage(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified),
                    excludeSelf = true,
                ).count(),
                EffectTarget.PlayerRef(Player.EachOpponent),
            )
        description = "Whenever this creature attacks, put a +1/+1 counter on target creature you control. " +
            "Then this creature deals X damage to each opponent, where X is the number of modified " +
            "creatures you control other than this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec1a2920-6d39-4276-921c-65cc16a45f17.jpg?1783923858"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
