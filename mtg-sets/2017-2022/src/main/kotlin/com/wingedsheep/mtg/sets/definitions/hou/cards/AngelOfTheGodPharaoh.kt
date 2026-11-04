package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val AngelOfTheGodPharaoh = card("Angel of the God-Pharaoh") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\nCycling {2} ({2}, Discard this card: Draw a card.)"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "E. M. Gist"
        flavorText = "The angels did not deceive. From the start, they were clear their allegiance was to the God-Pharaoh and to him alone."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad202785-a91f-4dc8-bcc0-eb1bd49f162e.jpg?1783936066"
    }
}
