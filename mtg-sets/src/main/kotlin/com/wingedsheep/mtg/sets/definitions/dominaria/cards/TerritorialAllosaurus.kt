package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Territorial Allosaurus
 * {2}{G}{G}
 * Creature — Dinosaur
 * 5/5
 * Kicker {2}{G}
 * When this creature enters, if it was kicked, it fights another target creature.
 */
val TerritorialAllosaurus = card("Territorial Allosaurus") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 5
    oracleText = "Kicker {2}{G} (You may pay an additional {2}{G} as you cast this spell.)\nWhen this creature enters, if it was kicked, it fights another target creature."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}{G}")))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        val t = target("another target creature", TargetCreature(filter = TargetFilter.OtherCreature))
        effect = Effects.Fight(EffectTarget.Self, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Jonathan Kuo"
        flavorText = "A living remnant of the Ice Age."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d8ac2b0-47ea-4aa3-bf24-c78227a0c913.jpg?1562734382"
    }
}
