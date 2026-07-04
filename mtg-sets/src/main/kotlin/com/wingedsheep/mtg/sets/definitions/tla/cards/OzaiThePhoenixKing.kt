package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ConvertEmptyingManaToRed
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ozai, the Phoenix King
 * {2}{B}{B}{R}{R}
 * Legendary Creature — Human Noble
 * 7/7
 *
 * Trample, firebending 4, haste
 * If you would lose unspent mana, that mana becomes red instead.
 * Ozai has flying and indestructible as long as you have six or more unspent mana.
 *
 * Modeling notes:
 *  - Trample and haste are plain keywords; `firebending(4)` is the set combat-mana helper
 *    (adds the keyword + the "whenever this attacks, add {R}{R}{R}{R} until end of combat" trigger).
 *  - "If you would lose unspent mana, that mana becomes red instead" is the [ConvertEmptyingManaToRed]
 *    static ability — the colour-converting cousin of Upwelling's `PreventManaPoolEmptying`. At the
 *    mana-empty point the controller's whole pool becomes that many red mana rather than emptying
 *    (honoured in CleanupPhaseManager).
 *  - "Ozai has flying and indestructible as long as you have six or more unspent mana" is two
 *    [ConditionalStaticAbility] grants (flying, indestructible) on the source, each gated by
 *    [Conditions.YouHaveUnspentManaAtLeast] (6), which reads the controller's total unspent mana.
 */
val OzaiThePhoenixKing = card("Ozai, the Phoenix King") {
    manaCost = "{2}{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Noble"
    power = 7
    toughness = 7
    oracleText = "Trample, firebending 4, haste\n" +
        "If you would lose unspent mana, that mana becomes red instead.\n" +
        "Ozai has flying and indestructible as long as you have six or more unspent mana."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)
    firebending(4)

    staticAbility {
        ability = ConvertEmptyingManaToRed
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = Conditions.YouHaveUnspentManaAtLeast(6)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.YouHaveUnspentManaAtLeast(6)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "235"
        artist = "Kekai Kotaki"
        flavorText = "\"Out of the ashes, a new world will be born.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98b1550-4609-4a2f-9371-4afe1cdc613e.jpg?1764121744"
    }
}
