package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Takeno, Samurai General
 * {5}{W}
 * Legendary Creature — Human Samurai
 * 3/3
 * Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)
 * Each other Samurai creature you control gets +1/+1 for each point of bushido it has.
 *
 * Bushido is lowered to its two triggers as in [NumaiOutcast]. The anthem reads each affected
 * Samurai's own bushido total through `KeywordValue(BUSHIDO)` on [EffectTarget.AffectedEntity], so
 * instances add and a Samurai that has lost its abilities gets nothing.
 */
val TakenoSamuraiGeneral = card("Takeno, Samurai General") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Samurai"
    power = 3
    toughness = 3
    oracleText = "Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)\n" +
        "Each other Samurai creature you control gets +1/+1 for each point of bushido it has."

    keywordAbility(KeywordAbility.bushido(2))

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Bushido 2"
    }

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Bushido 2"
    }

    staticAbility {
        val bonus = DynamicAmounts.propertyOf(
            EffectTarget.AffectedEntity,
            EntityNumericProperty.KeywordValue(Keyword.BUSHIDO)
        )
        ability = GrantDynamicStats(
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SAMURAI).youControl()).other(),
            powerBonus = bonus,
            toughnessBonus = bonus
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d038df46-4a30-4125-be0e-8781b16b523e.jpg?1783944332"
    }
}
