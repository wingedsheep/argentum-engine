package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val BattlewingMystic = card("Battlewing Mystic") {
    manaCost = "{1}{U}"
    colorIdentity = "RU"
    typeLine = "Creature — Bird Wizard"
    oracleText = "Kicker {R} (You may pay an additional {R} as you cast this spell.)\nFlying\nWhen this creature enters, if it was kicked, discard your hand, then draw two cards."
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.kicker("{R}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Patterns.Hand.discardHand() then Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/000376ef-8b6c-490d-98cb-d6de15b2e585.jpg?1783921354"
    }
}
