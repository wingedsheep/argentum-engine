package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Toph, the First Metalbender
 * {1}{R}{G}{W}
 * Legendary Creature — Human Warrior Ally
 * 3/3
 *
 * Nontoken artifacts you control are lands in addition to their other types. (They don't gain
 * the ability to {T} for mana.)
 * At the beginning of your end step, earthbend 2. (Target land you control becomes a 0/0 creature
 * with haste that's still a land. Put two +1/+1 counters on it. When it dies or is exiled, return
 * it to the battlefield tapped.)
 *
 * Modeling notes:
 * - "Nontoken artifacts you control are lands in addition to their other types" is a Layer 4
 *   (type-changing) static, expressed with [GrantAdditionalTypesToGroup] adding the LAND card
 *   type to the nontoken-artifacts-you-control group. Adding the land type alone grants no mana
 *   ability, which matches the reminder text.
 * - The end-step earthbend is the standard [Triggers.YourEndStep] + [Effects.Earthbend] pattern
 *   targeting a land you control.
 */
val TophTheFirstMetalbender = card("Toph, the First Metalbender") {
    manaCost = "{1}{R}{G}{W}"
    colorIdentity = "GRW"
    typeLine = "Legendary Creature — Human Warrior Ally"
    power = 3
    toughness = 3
    oracleText = "Nontoken artifacts you control are lands in addition to their other types. " +
        "(They don't gain the ability to {T} for mana.)\n" +
        "At the beginning of your end step, earthbend 2. (Target land you control becomes a 0/0 " +
        "creature with haste that's still a land. Put two +1/+1 counters on it. When it dies or " +
        "is exiled, return it to the battlefield tapped.)"

    // Nontoken artifacts you control are lands in addition to their other types.
    staticAbility {
        ability = GrantAdditionalTypesToGroup(
            filter = GroupFilter(GameObjectFilter.Artifact.youControl().nontoken()),
            addCardTypes = listOf("LAND")
        )
    }

    // At the beginning of your end step, earthbend 2.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        val t = target("target", TargetPermanent(filter = TargetFilter.Land.youControl()))
        effect = Effects.Earthbend(2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Eilene Cherie"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70b6670f-a9fa-4d75-b0a7-c01b5071a514.jpg?1764121822"
    }
}
