package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtectionToController
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Absolute Virtue
 * {6}{W}{U}
 * Legendary Creature — Avatar Warrior
 * 8/8
 * This spell can't be countered.
 * Flying
 * You have protection from each of your opponents. (You can't be dealt damage, enchanted, or
 * targeted by anything controlled by your opponents.)
 *
 * Modeling: "can't be countered" is the intrinsic spell flag ([cantBeCountered]); flying is an
 * intrinsic keyword; the protection clause grants the *controller* (not the creature) player-level
 * protection via the continuous static [GrantProtectionToController] with
 * [ProtectionScope.EachOpponent] — the static counterpart of The One Ring's one-shot
 * `GrantPlayerProtectionEffect`. The protection lasts only while Absolute Virtue is on the
 * battlefield.
 */
val AbsoluteVirtue = card("Absolute Virtue") {
    manaCost = "{6}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Avatar Warrior"
    power = 8
    toughness = 8
    oracleText = "This spell can't be countered.\n" +
        "Flying\n" +
        "You have protection from each of your opponents. (You can't be dealt damage, enchanted, " +
        "or targeted by anything controlled by your opponents.)"

    cantBeCountered = true

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantProtectionToController(ProtectionScope.EachOpponent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "212"
        artist = "Toni Infante"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa192912-c9ee-403f-8a46-a338c9edb4b9.jpg?1782686436"
    }
}
