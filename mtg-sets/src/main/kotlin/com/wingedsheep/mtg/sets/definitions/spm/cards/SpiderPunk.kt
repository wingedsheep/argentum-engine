package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Spider-Punk (Marvel's Spider-Man, #92)
 * {1}{R}
 * Legendary Creature — Spider Human Hero
 * 2/1
 *
 * Riot (This creature enters with your choice of a +1/+1 counter or haste.)
 * Other Spiders you control have riot.
 * Spells and abilities can't be countered.
 * Damage can't be prevented.
 *
 *  - **Riot** — the `riot()` DSL helper (Siege-style `EntersWithChoice(MODE)` + mode-gated counter /
 *    haste).
 *  - **Other Spiders you control have riot** — `GrantKeyword(Keyword.RIOT, other Spiders you
 *    control)`. The engine synthesizes the enters-with choice for any Spider that enters carrying
 *    the granted RIOT keyword (granted-riot synthesis at the entry seams).
 *  - **Spells and abilities can't be countered** — a symmetric `GrantCantBeCountered(Any,
 *    includesAbilities = true)`; every spell AND ability on the stack is uncounterable (e.g. Stifle
 *    fizzles) while this is in play.
 *  - **Damage can't be prevented** — a global `DamageCantBePrevented` replacement.
 */
val SpiderPunk = card("Spider-Punk") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 2
    toughness = 1
    oracleText = "Riot (This creature enters with your choice of a +1/+1 counter or haste.)\n" +
        "Other Spiders you control have riot.\n" +
        "Spells and abilities can't be countered.\n" +
        "Damage can't be prevented."

    // Riot (printed).
    riot()

    // Other Spiders you control have riot.
    staticAbility {
        ability = GrantKeyword(
            Keyword.RIOT,
            GroupFilter(GameObjectFilter.Creature.withSubtype("Spider").youControl(), excludeSelf = true),
        )
    }

    // Spells and abilities can't be countered.
    staticAbility {
        ability = GrantCantBeCountered(GameObjectFilter.Any, includesAbilities = true)
    }

    // Damage can't be prevented.
    replacementEffect(DamageCantBePrevented())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Forrest Imel"
        flavorText = "\"Disrespect authority. Smash the system.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bd41879-fcd4-4211-9b98-47e7cdba5399.jpg?1783905333"
    }
}
