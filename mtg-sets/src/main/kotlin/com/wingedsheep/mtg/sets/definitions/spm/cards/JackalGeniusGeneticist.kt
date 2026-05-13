package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

val JackalGeniusGeneticist = card("Jackal, Genius Geneticist") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Scientist Villain"
    power = 1
    toughness = 1
    oracleText = "Trample\nWhenever you cast a creature spell with mana value equal to Jackal's power, copy that spell, except the copy isn't legendary. Then put a +1/+1 counter on Jackal. (The copy becomes a token.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        triggerCondition = Compare(
            DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.ManaValue),
            ComparisonOperator.EQ,
            DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power)
        )
        effect = Effects.Composite(
            Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity, stripSupertypes = true),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Pavel Kolomeyets"
        flavorText = "For Professor Miles Warren, all any problem needs is a lot of clones and not a lot of ethics."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0ab07d6-b7c3-4129-9aef-cfdcfabec4b2.jpg?1757377692"
    }
}
