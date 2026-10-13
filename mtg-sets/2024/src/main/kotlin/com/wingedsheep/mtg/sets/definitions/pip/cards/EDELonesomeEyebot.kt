package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * ED-E, Lonesome Eyebot — Fallout #131.
 *
 * "The number of attacking creatures" counts every attacking creature, not just yours; it is an
 * intervening-if, checked when you attack and again on resolution (the ruling). The sacrifice
 * ability reads ED-E's quest counters as it last existed on the battlefield, since it was
 * sacrificed to pay the cost.
 */
val EDELonesomeEyebot = card("ED-E, Lonesome Eyebot") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 2
    toughness = 1
    oracleText = "Flying\nED-E My Love — Whenever you attack, if the number of attacking creatures is greater than the number of quest counters on ED-E, put a quest counter on it.\n{2}, Sacrifice ED-E: Draw a card, then draw an additional card for each quest counter on ED-E."
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.you.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count(),
            ComparisonOperator.GT,
            DynamicAmounts.countersOnSelf(CounterType.QUEST),
        )
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(DynamicAmounts.lastKnownSourceCounters(CounterType.QUEST) + 1)
        description = "Draw a card, then draw an additional card for each quest counter on ED-E."
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Leanna Crossan"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b319f0e1-6f18-4fea-9f72-a21da5513aa4.jpg?1783912378"
        ruling("2024-03-08", "If the number of attacking creatures isn't greater than the number of quest counters on ED-E when you attack, its second ability won't trigger at all. When the ability tries to resolve, if the number of attacking creatures is no longer greater than the number of quest counters on ED-E, it won't resolve, and you won't put a quest counter on ED-E.")
    }
}
