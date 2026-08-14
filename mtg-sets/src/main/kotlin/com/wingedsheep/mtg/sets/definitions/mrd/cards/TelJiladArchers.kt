package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Tel-Jilad Archers — Mirrodin #131
 * {4}{G} · Creature — Elf Archer · 2/4
 *
 * Protection from artifacts; reach
 *
 * The Tangle's anti-air battery, and the big brother of [TelJiladChosen]. Both halves are printed
 * keywords: reach via [Keyword.REACH], and protection from artifacts via the card-type protection
 * scope ([ProtectionScope.CardType]), projected as `PROTECTION_FROM_CARDTYPE_ARTIFACT`. Targeting,
 * damage prevention, and block evasion all read that projected keyword, so an artifact creature
 * can't block it, an artifact source can't damage it, and an artifact's ability can't target it —
 * no per-card wiring needed.
 *
 * Note the two keywords pull in opposite directions in combat: reach lets it block fliers, while
 * protection from artifacts means an artifact flier it blocks deals it no damage at all.
 */
val TelJiladArchers = card("Tel-Jilad Archers") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Archer"
    power = 2
    toughness = 4
    oracleText = "Protection from artifacts; reach (This creature can block creatures with flying.)"

    keywords(Keyword.REACH)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Artifact")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Marcelo Vignali"
        flavorText = "They are extensions of the Tangle, stretching its vines into the furthest " +
            "reaches of the sky."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a1fde33-e86a-4146-9c90-4616a5fc0868.jpg?1783944531"
    }
}
