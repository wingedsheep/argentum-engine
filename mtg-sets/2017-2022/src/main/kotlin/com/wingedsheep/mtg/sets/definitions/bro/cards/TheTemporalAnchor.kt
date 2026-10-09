package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

val TheTemporalAnchor = card("The Temporal Anchor") {
    manaCost = "{3}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact"
    oracleText = "At the beginning of your upkeep, scry 2.\nWhenever you choose to put one or more cards on the bottom of your library while scrying, exile that many cards from the bottom of your library.\nDuring your turn, you may play cards exiled with The Temporal Anchor."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Scry(2)
    }

    triggeredAbility {
        trigger = Triggers.you.scries()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.triggerScryBottomCount(), ComparisonOperator.GT, 0
        )
        effect = Effects.Pipeline {
            val bottom = gather(CardSource.BottomOfLibrary(DynamicAmounts.triggerScryBottomCount()))
            exile(bottom, linkToSource = true)
        }
    }

    staticAbility {
        ability = GrantMayCastFromLinkedExile(
            filter = GameObjectFilter.Any,
            duringYourTurnOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a212d59-b72e-4939-a757-c131ca4323ce.jpg?1783920095"
        ruling("2022-10-14", "You must follow all normal timing rules for a card you play using The Temporal Anchor's last ability and, if it's a spell, you must pay its costs to cast it.")
    }
}
