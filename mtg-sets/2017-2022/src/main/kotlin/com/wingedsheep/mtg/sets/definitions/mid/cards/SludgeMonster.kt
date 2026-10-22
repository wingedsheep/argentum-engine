package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sludge Monster (Innistrad: Midnight Hunt #76) — {3}{U}{U} Creature — Horror, 5/5
 *
 *   Whenever this creature enters or attacks, put a slime counter on up to one other target creature.
 *   Non-Horror creatures with slime counters on them lose all abilities and have base power and
 *   toughness 2/2.
 *
 * "Enters or attacks" is two triggers, each with an optional ("up to one") other-creature target.
 * The static reads every creature on the battlefield (any controller, any source of the slime
 * counter, per the ruling) through the projected subtype set, so a creature that becomes a Horror
 * stops being affected. Layer 6 strips abilities; layer 7b sets base P/T, so counters and pumps
 * still apply on top.
 */
val SludgeMonster = card("Sludge Monster") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Horror"
    power = 5
    toughness = 5
    oracleText = "Whenever this creature enters or attacks, put a slime counter on up to one other " +
        "target creature.\n" +
        "Non-Horror creatures with slime counters on them lose all abilities and have base power " +
        "and toughness 2/2."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.AddCounters(CounterType.SLIME, 1, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.AddCounters(CounterType.SLIME, 1, creature)
    }

    val slimedNonHorrors = GroupFilter(
        GameObjectFilter.Creature.notSubtype(Subtype.HORROR).withCounter(CounterType.SLIME)
    )

    staticAbility { ability = LoseAllAbilities(filter = slimedNonHorrors) }
    staticAbility {
        ability = SetBasePowerToughnessStatic(power = 2, toughness = 2, filter = slimedNonHorrors)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Svetlin Velinov"
        flavorText = "Tracking it isn't the hard part."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57fa9aa8-86d2-4f1e-ac65-ace7e73a853c.jpg?1783925630"
        ruling(
            "2021-09-24",
            "If a non-Horror creature with a slime counter on it somehow becomes a Horror, the effect making it " +
                "2/2 and making it lose its abilities ends. Its base power and toughness will revert to whatever " +
                "it was before that effect started applying, although other effects that modify power and/or " +
                "toughness, such as ones created by Auras, counters, and the like may still apply. It'll also " +
                "have whatever abilities it had before it lost them."
        )
        ruling(
            "2021-09-24",
            "Sludge Monster's last ability affects all non-Horror creatures with slime counters on them, not just " +
                "creatures that had slime counters put on them with Sludge Monster's first ability."
        )
    }
}
