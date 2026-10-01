package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rebel Salvo
 * {2}{R}
 * Instant
 *
 * Affinity for Equipment (This spell costs {1} less to cast for each Equipment you control.)
 * Rebel Salvo deals 5 damage to target creature or planeswalker. That permanent loses
 * indestructible until end of turn.
 *
 * Both halves happen during resolution, before state-based actions are checked, so an
 * indestructible creature dealt lethal damage is destroyed.
 */
val RebelSalvo = card("Rebel Salvo") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Affinity for Equipment (This spell costs {1} less to cast for each Equipment you control.)\n" +
        "Rebel Salvo deals 5 damage to target creature or planeswalker. That permanent loses indestructible until end of turn."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.EQUIPMENT))

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(5, victim) then
            Effects.RemoveKeyword(Keyword.INDESTRUCTIBLE, victim, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Khurrum"
        flavorText = "\"That Great Work of yours is looking pretty fragile, Urabrask!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd37d1b1-70ce-466e-890d-36be82433035.jpg?1783918025"
    }
}
