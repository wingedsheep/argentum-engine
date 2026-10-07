package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val InnerDemon = card("Inner Demon") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2, has flying, and is a Demon in addition to its other types.\n" +
        "When this Aura enters, all non-Demon creatures get -2/-2 until end of turn."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility { ability = ModifyStats(2, 2) }
    staticAbility { ability = GrantKeyword(Keyword.FLYING) }
    staticAbility { ability = GrantSubtype("Demon", Filters.EnchantedCreature) }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.DEMON)),
            effect = Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcbfc305-f3b3-4f8f-b18f-bb7af01a39a4.jpg?1783934861"
        ruling(
            "2018-06-08",
            "Inner Demon's triggered ability affects only non-Demon creatures on the battlefield at the time it resolves. " +
                "Creatures that enter the battlefield later in the turn won't get -2/-2. " +
                "Unless Inner Demon leaves the battlefield before that ability resolves, the enchanted creature won't get -2/-2."
        )
    }
}
