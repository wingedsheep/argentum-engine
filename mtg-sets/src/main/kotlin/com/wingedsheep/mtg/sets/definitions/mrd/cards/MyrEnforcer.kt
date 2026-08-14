package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Myr Enforcer — Mirrodin #211
 * {7} · Artifact Creature — Myr · 4/4
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 *
 * The top of the affinity curve alongside [Frogmite] — the whole cost is generic, so seven
 * artifacts make a 4/4 free. Nothing but the stock [KeywordAbility.Affinity] over
 * [CardType.ARTIFACT]; affinity only shaves generic mana and there is nothing else here to shave.
 *
 * Affinity is a cost reduction, not an alternative cost: the mana value stays 7 in every zone no
 * matter how cheaply it was cast, and the artifact count is taken as the total cost is locked in
 * while casting.
 */
val MyrEnforcer = card("Myr Enforcer") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Myr"
    power = 4
    toughness = 4
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)"

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "211"
        artist = "Greg Staples"
        flavorText = "Most myr monitor other species. Some myr monitor other myr."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeeef899-a8b5-4416-a208-3bd5a0c7177b.jpg?1783944512"
    }
}
