package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val BlueSunsTwilight = card("Blue Sun's Twilight") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature with mana value X or less. If X is 5 or more, create a token that's a copy of that creature."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.manaValueAtMostX()))
        effect = Effects.GainControl(t) then
            ConditionalEffect(
                condition = Compare(DynamicAmount.XValue, ComparisonOperator.GTE, DynamicAmount.Fixed(5)),
                effect = Effects.CreateTokenCopyOfTarget(t)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Piotr Dura"
        flavorText = "\"Where once there was ignorance, Jin-Gitaxias brought knowledge, and an age of progress dawned.\"\n—Monument inscription"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a836e6b-6854-4f6f-bc78-2da1fd4dd224.jpg?1675956948"
    }
}
