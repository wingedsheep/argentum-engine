package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val DocOcksHenchmen = card("Doc Ock's Henchmen") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Villain"
    power = 2
    toughness = 1
    oracleText = "Flash\nWhenever this creature attacks, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Nathaniel Himawan"
        flavorText = "\"Don't be a hero, Gary! We don't get paid enough for a scrap with Spider-Man!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a383c442-3f4a-4115-97e4-23f0eb88465b.jpg?1783905355"
    }
}
