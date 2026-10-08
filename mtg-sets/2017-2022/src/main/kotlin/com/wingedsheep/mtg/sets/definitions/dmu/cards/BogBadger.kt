package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val BogBadger = card("Bog Badger") {
    manaCost = "{2}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Badger"
    oracleText = "Kicker {B} (You may pay an additional {B} as you cast this spell.)\nWhen this creature enters, if it was kicked, creatures you control gain menace until end of turn. (A creature with menace can't be blocked except by two or more creatures.)"
    power = 3
    toughness = 3

    keywordAbility(KeywordAbility.kicker("{B}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Patterns.Group.grantKeywordToAll(Keyword.MENACE, Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d31d878-9114-4205-b7a9-bb13ce6aedd2.jpg?1783921303"
    }
}
