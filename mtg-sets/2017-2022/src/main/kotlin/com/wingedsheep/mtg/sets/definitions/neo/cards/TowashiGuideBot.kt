package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Towashi Guide-Bot — Kamigawa: Neon Dynasty #262 (canonical printing)
 * {4} · Artifact Creature — Construct · 2/1
 *
 * When this creature enters, put a +1/+1 counter on target creature you control.
 * {4}, {T}: Draw a card. This ability costs {1} less to activate for each modified creature you
 * control.
 *
 * The discount is the activated ability's `genericCostReduction` (the channel-land shape) over the
 * count of creatures you control matching [StatePredicate.IsModified] (CR 700.9). The ETB counter
 * can land on the Guide-Bot itself, making it modified.
 */
val TowashiGuideBot = card("Towashi Guide-Bot") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, put a +1/+1 counter on target creature you control.\n" +
        "{4}, {T}: Draw a card. This ability costs {1} less to activate for each modified creature " +
        "you control. (Equipment, Auras you control, and counters are modifications.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        genericCostReduction = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified),
        ).count()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "262"
        artist = "Jinho Bae"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da4ccf70-b661-46ea-bea9-4466a3a35562.jpg?1783923819"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
