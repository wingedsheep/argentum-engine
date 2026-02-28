package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Decree of Savagery
 * {7}{G}{G}
 * Instant
 * Put four +1/+1 counters on each creature you control.
 * Cycling {4}{G}{G}
 * When you cycle Decree of Savagery, you may put four +1/+1 counters on target creature.
 */
val DecreeOfSavagery = card("Decree of Savagery") {
    manaCost = "{7}{G}{G}"
    typeLine = "Instant"
    oracleText = "Put four +1/+1 counters on each creature you control.\nCycling {4}{G}{G}\nWhen you cycle Decree of Savagery, you may put four +1/+1 counters on target creature."

    spell {
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = AddCountersEffect(
                counterType = "+1/+1",
                count = 4,
                target = EffectTarget.Self
            )
        )
    }

    keywordAbility(KeywordAbility.cycling("{4}{G}{G}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val t = target("target creature", Targets.Creature)
        effect = MayEffect(
            AddCountersEffect(
                counterType = "+1/+1",
                count = 4,
                target = t
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e643fbf1-74d5-412b-beba-ab3c712edb3b.jpg?1562536230"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
        ruling("2022-12-08", "You can cycle this card even if there are no legal targets for the triggered ability.")
    }
}
