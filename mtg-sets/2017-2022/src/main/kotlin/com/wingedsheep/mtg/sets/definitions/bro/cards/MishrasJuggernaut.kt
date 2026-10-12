package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Mishra's Juggernaut
 * {5}
 * Artifact Creature — Juggernaut
 * 5/3
 * Trample
 * This creature attacks each combat if able.
 * Unearth {5}{R}
 */
val MishrasJuggernaut = card("Mishra's Juggernaut") {
    manaCost = "{5}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Juggernaut"
    power = 5
    toughness = 3
    oracleText = "Trample\n" +
        "This creature attacks each combat if able.\n" +
        "Unearth {5}{R} ({5}{R}: Return this card from your graveyard to the battlefield. It gains haste. Exile it " +
        "at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = MustAttack()
    }

    unearth("{5}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b30457c3-57df-4720-8b45-962f16316d17.jpg?1783920058"
        ruling("2022-10-14", "If Mishra's Juggernaut can't attack for any reason (such as being tapped), then it doesn't attack. If there's a cost associated with having it attack, its controller isn't forced to pay that cost, so it doesn't have to attack in that case either.")
        ruling("2022-10-14", "If you activate a card's unearth ability but that card is removed from your graveyard before the ability resolves, that unearth ability will do nothing as it resolves.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it!")
    }
}
