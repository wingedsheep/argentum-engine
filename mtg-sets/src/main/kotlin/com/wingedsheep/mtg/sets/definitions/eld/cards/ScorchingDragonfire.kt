package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect

/**
 * Scorching Dragonfire — Throne of Eldraine #139 (canonical printing)
 * {1}{R} · Instant
 *
 * Scorching Dragonfire deals 3 damage to target creature or planeswalker. If that creature or
 * planeswalker would die this turn, exile it instead.
 *
 * The spell deals 3 damage then marks the target with [MarkExileOnDeathEffect], a death-replacement
 * (CR 614) that exiles it instead of letting it go to the graveyard this turn — so even lethal
 * damage from another source sends it to exile. The marker expires at end of turn if it survives.
 *
 * Earliest real expansion printing is ELD (2019); reprinted in M21, J22, FDN, and DSK among others.
 */
val ScorchingDragonfire = card("Scorching Dragonfire") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Scorching Dragonfire deals 3 damage to target creature or planeswalker. " +
        "If that creature or planeswalker would die this turn, exile it instead."

    spell {
        val t = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        effect = DealDamageEffect(3, t) then MarkExileOnDeathEffect(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Eric Velhagen"
        flavorText = "As the blaze raced toward her, Syr Ameril closed her eyes and pictured " +
            "Castle Ardenvale so her last thoughts would be of home."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b74a806-ed74-458e-8903-d3d084e9f507.jpg?1636491470"
    }
}
