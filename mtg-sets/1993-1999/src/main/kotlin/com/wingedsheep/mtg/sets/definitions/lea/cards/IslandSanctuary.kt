package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeAttackedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val IslandSanctuary = card("Island Sanctuary") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment"
    oracleText = "If you would draw a card during your draw step, instead you may skip that draw. If you do, until your next turn, you can't be attacked except by creatures with flying and/or islandwalk."

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.GrantStaticAbility(
                CantBeAttackedBy(GameObjectFilter.Creature
                    .withoutKeyword(Keyword.FLYING).withoutKeyword(Keyword.ISLANDWALK)),
                EffectTarget.Controller,
                Duration.UntilYourNextTurn,
            ),
            optional = true,
            restrictions = listOf(Conditions.IsInStep(Step.DRAW)),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c15e8a42-89de-42bc-8d5f-33426d207c3a.jpg?1783948712"
        ruling("2004-10-04", "If you get multiple draws or you use a spell or ability during the draw step to draw extra cards, you can have the replacement effect apply to any one or all of those. You need only have it apply once to get the effect. If you skip more than one, there is no additional effect.")
        ruling("2004-10-04", "If the replacement effect is applied, the effect will continue until your next turn even if this card leaves the battlefield.")
        ruling("2004-10-04", "Since the draw is replaced, you can't use the same draw to do other things.")
    }
}
