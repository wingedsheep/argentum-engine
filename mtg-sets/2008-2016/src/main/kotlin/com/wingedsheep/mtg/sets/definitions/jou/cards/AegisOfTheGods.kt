package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofToController

/**
 * Aegis of the Gods
 * {1}{W}
 * Enchantment Creature — Human Soldier
 * 2/1
 * You have hexproof. (You can't be the target of spells or abilities your opponents control.)
 *
 * "You have hexproof" is the [GrantHexproofToController] static (Leyline of Sanctity,
 * Keen-Eared Sentry).
 */
val AegisOfTheGods = card("Aegis of the Gods") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Soldier"
    oracleText = "You have hexproof. (You can't be the target of spells or abilities your opponents control.)"
    power = 2
    toughness = 1

    staticAbility {
        ability = GrantHexproofToController
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Yefim Kligerman"
        flavorText = "Athreos cares little for the other gods' conflict with mortals. He is concerned only with safe passage for the dead."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2b2f381-86a2-42ac-b694-dcde437d574f.jpg?1783939463"
    }
}
