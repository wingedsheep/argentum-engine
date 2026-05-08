package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Tifa Lockhart
 * {1}{G}
 * Legendary Creature — Human Monk
 * 1/2
 * Trample
 * Landfall — Whenever a land you control enters, double Tifa Lockhart's power until end of turn.
 */
val TifaLockhart = card("Tifa Lockhart") {
    manaCost = "{1}{G}"
    typeLine = "Legendary Creature — Human Monk"
    power = 1
    toughness = 2
    oracleText = "Trample\nLandfall — Whenever a land you control enters, double Tifa Lockhart's power until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = ModifyStatsEffect(
            powerModifier = DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power),
            toughnessModifier = DynamicAmount.Fixed(0),
            target = EffectTarget.Self
        )
        description = "Whenever a land you control enters, double Tifa Lockhart's power until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Laurel Austin"
        flavorText = "\"I'm pretty good at taking care of myself, you know.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb781323-2746-405d-a9b2-e778c037a6e9.jpg?1748706535"
    }
}
