package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAsThoughHadReach
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Dragon Hunter
 * {W}
 * Creature — Human Warrior
 * 2/1
 * Protection from Dragons
 * This creature can block Dragons as though it had reach.
 *
 * The reach permission is scoped to Dragon attackers: it answers only the flying/reach blocking
 * question for them, and the creature never gains reach itself.
 */
val DragonHunter = card("Dragon Hunter") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Protection from Dragons\nThis creature can block Dragons as though it had reach."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Subtype("Dragon")))

    staticAbility {
        ability = CanBlockAsThoughHadReach(GameObjectFilter.Creature.withSubtype("Dragon"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae8a0362-b973-445a-a355-5a2f3dc67f16.jpg?1783938619"
    }
}
