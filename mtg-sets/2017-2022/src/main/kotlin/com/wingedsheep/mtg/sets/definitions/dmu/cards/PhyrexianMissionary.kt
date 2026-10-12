package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val PhyrexianMissionary = card("Phyrexian Missionary") {
    manaCost = "{1}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Phyrexian Human Cleric"
    power = 2
    toughness = 3
    oracleText = "Kicker {1}{B} (You may pay an additional {1}{B} as you cast this spell.)\nLifelink\nWhen this creature enters, if it was kicked, return target creature card from your graveyard to your hand."

    keywordAbility(KeywordAbility.kicker("{1}{B}"))
    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f22fcfa-a5f0-4fae-b578-cdfd7d46a4c8.jpg?1783921362"
    }
}
