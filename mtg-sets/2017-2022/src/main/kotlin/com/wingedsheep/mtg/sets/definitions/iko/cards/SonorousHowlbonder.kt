package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sonorous Howlbonder
 * {1}{B/R}{B/R}
 * Creature — Human Warrior
 * 2/2
 * Menace
 * Each creature you control with menace can't be blocked except by three or more creatures.
 *
 * The group clause covers Howlbonder itself and any creature that has menace right now (printed or
 * granted), read from projected state at block declaration.
 */
val SonorousHowlbonder = card("Sonorous Howlbonder") {
    manaCost = "{1}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Each creature you control with menace can't be blocked except by three or more creatures."

    keywords(Keyword.MENACE)

    staticAbility {
        ability = CantBeBlockedByFewerThan(
            minBlockers = 3,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.MENACE)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Kimonas Theodossiou"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/595f0b25-6e6e-43c4-a4a0-903920067df1.jpg?1783931008"
    }
}
