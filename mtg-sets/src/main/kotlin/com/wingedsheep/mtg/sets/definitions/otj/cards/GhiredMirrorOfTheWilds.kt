package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Ghired, Mirror of the Wilds
 * {R}{G}{W}
 * Legendary Creature — Human Shaman
 * 3/3
 *
 * Haste
 * Nontoken creatures you control have "{T}: Create a token that's a copy of target token
 * you control that entered this turn."
 *
 * The granted ability is a lord-style [GrantActivatedAbility] over `nontoken creatures you
 * control`. Each granted instance taps its own bearer ([Costs.Tap]) and targets a *token*
 * permanent the controller owns that entered the battlefield this turn
 * (`GameObjectFilter.Token.youControl().enteredThisTurn()`), then makes a copy of it via
 * [Effects.CreateTokenCopyOfTarget].
 */
val GhiredMirrorOfTheWilds = card("Ghired, Mirror of the Wilds") {
    manaCost = "{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Human Shaman"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Nontoken creatures you control have \"{T}: Create a token that's a copy of " +
        "target token you control that entered this turn.\""

    keywords(Keyword.HASTE)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.CreateTokenCopyOfTarget(EffectTarget.ContextTarget(0)),
                targetRequirement = TargetPermanent(
                    filter = TargetFilter(
                        GameObjectFilter.Token.youControl().enteredThisTurn(),
                    ),
                ),
            ),
            filter = GroupFilter(GameObjectFilter.Creature.youControl().nontoken()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "205"
        artist = "Diego Gisbert"
        flavorText = "\"The Conclave was soft. The Clans were trapped beasts. Here in this " +
            "untamed wilderness, nature can truly roam free.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e43e3d71-4fb8-4ab1-8c8f-b65ae3ad4cc4.jpg?1712356098"
    }
}
