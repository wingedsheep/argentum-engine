package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Exuberant Fuseling — Phyrexia: All Will Be One #129
 * {R} · Creature — Phyrexian Goblin Warrior · 0/1 · Uncommon
 *
 * Trample
 * This creature gets +1/+0 for each oil counter on it.
 * When this creature enters and whenever another creature or artifact you control is put into a
 * graveyard from the battlefield, put an oil counter on this creature.
 *
 * The combined "When … and whenever …" sentence is two triggers sharing one effect.
 */
val ExuberantFuseling = card("Exuberant Fuseling") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Goblin Warrior"
    power = 0
    toughness = 1
    oracleText = "Trample\n" +
        "This creature gets +1/+0 for each oil counter on it.\n" +
        "When this creature enters and whenever another creature or artifact you control is put " +
        "into a graveyard from the battlefield, put an oil counter on this creature."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.countersOnSelf(CounterType.OIL),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "When this creature enters, put an oil counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.CreatureOrArtifact.youControl()).dies()
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "Whenever another creature or artifact you control is put into a graveyard " +
            "from the battlefield, put an oil counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6e8d7b9-ff5e-48ae-9e38-d2b6a45b119b.jpg?1783918031"
    }
}
