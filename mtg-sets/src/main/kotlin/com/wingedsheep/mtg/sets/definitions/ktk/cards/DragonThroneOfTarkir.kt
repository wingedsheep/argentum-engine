package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects

/**
 * Dragon Throne of Tarkir
 * {4}
 * Legendary Artifact — Equipment
 * Equipped creature has defender and "{2}, {T}: Other creatures you control gain trample
 * and get +X/+X until end of turn, where X is this creature's power."
 * Equip {3}
 *
 * Ruling (2017-07-14): If the equipped creature's power is negative while resolving the
 * activated ability it gains, X is considered to be 0.
 * Ruling (2014-09-20): The value of X is determined as the activated ability resolves.
 * If the equipped creature isn't on the battlefield at that time, use its power the last
 * time it was on the battlefield to determine the value of X.
 */
val DragonThroneOfTarkir = card("Dragon Throne of Tarkir") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature has defender and \"{2}, {T}: Other creatures you control gain trample and get +X/+X until end of turn, where X is this creature's power.\"\nEquip {3}"

    // Equipped creature has defender
    staticAbility {
        ability = GrantKeyword(Keyword.DEFENDER)
    }

    // Equipped creature has "{2}, {T}: Other creatures you control gain trample and get +X/+X
    // until end of turn, where X is this creature's power."
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap),
                effect = Effects.Composite(
                    listOf(
                        Effects.ForEachInGroup(
                            GroupFilter.OtherCreaturesYouControl,
                            GrantKeywordEffect(Keyword.TRAMPLE, EffectTarget.Self)
                        ),
                        Effects.ForEachInGroup(
                            GroupFilter.OtherCreaturesYouControl,
                            ModifyStatsEffect(DynamicAmounts.sourcePower(), DynamicAmounts.sourcePower(), EffectTarget.Self)
                        )
                    )
                ),
                descriptionOverride = "{2}, {T}: Other creatures you control gain trample and get +X/+X until end of turn, where X is this creature's power."
            )
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Daarken"
        flavorText = "What once soared high above Tarkir is now reduced to a seat."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d508e6a6-034c-424d-993e-7354ce212f13.jpg?1562794087"
        ruling("2017-07-14", "If the equipped creature's power is negative while resolving the activated ability it gains, X is considered to be 0.")
        ruling("2014-09-20", "The value of X is determined as the activated ability resolves. If the equipped creature isn't on the battlefield at that time, use its power the last time it was on the battlefield to determine the value of X.")
    }
}
