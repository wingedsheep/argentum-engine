package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kree Commandos
 * {2}{W}
 * Creature — Kree Soldier Villain
 * 2/1
 * Flying, vigilance
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 *
 * Implementation note: flying and vigilance are plain keywords; prowess goes through the
 * `prowess()` CardBuilder helper, which adds the keyword *and* the intrinsic triggered ability
 * (the bare keyword would render but never fire).
 */
val KreeCommandos = card("Kree Commandos") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kree Soldier Villain"
    oracleText = "Flying, vigilance\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"
    power = 2
    toughness = 1
    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    prowess()
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Aaron J. Riley"
        flavorText = "The Kree Empire has existed for millennia. With each world it conquers, it finds new ways to fight."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/401f9ccf-f30c-4777-9a5b-4c340e724fff.jpg?1783902972"
    }
}
