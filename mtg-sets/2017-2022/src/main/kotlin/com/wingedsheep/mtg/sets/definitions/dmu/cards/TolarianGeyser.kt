package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val TolarianGeyser = card("Tolarian Geyser") {
    manaCost = "{2}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Kicker {W} (You may pay an additional {W} as you cast this spell.)\nReturn target creature to its owner's hand. Draw a card. If this spell was kicked, you gain 3 life."

    keywordAbility(KeywordAbility.kicker("{W}"))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then Effects.DrawCards(1) then
            Effects.If(Conditions.WasKicked, Effects.GainLife(3))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Olivier Bernard"
        flavorText = "\"If they don't drown, perhaps they'll rust.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dd5f389-fea2-4aed-a218-eca162902775.jpg?1783921342"
    }
}
