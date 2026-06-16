package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Enraged Huorn
 * {4}{G}
 * Creature — Treefolk
 * 4/5
 *
 * Trample
 * When this creature enters, the Ring tempts you.
 */
val EnragedHuorn = card("Enraged Huorn") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 4
    toughness = 5
    oracleText = "Trample\nWhen this creature enters, the Ring tempts you."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "David Álvarez"
        flavorText = "\"They stand watching endlessly over the trees. It is difficult to see them moving, but they can move very quickly if they are angry.\"\n—Merry"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9b48836-6d37-46eb-8c41-a3eeecc72ae1.jpg?1686969323"
    }
}
