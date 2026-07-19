package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pym Particles
 * {U}
 * Sorcery
 *
 * Target creature gains vigilance until end of turn and can't be blocked this turn.
 * Draw a card.
 *
 * Both grants default to [com.wingedsheep.sdk.scripting.Duration.EndOfTurn] — "until end of turn"
 * and "this turn" are the same window here. "Can't be blocked" is the
 * [AbilityFlag.CANT_BE_BLOCKED] evasion flag rather than a keyword. The spell has a single target,
 * so if that creature becomes an illegal target the whole spell is countered on resolution
 * (CR 608.2b) and no card is drawn.
 */
val PymParticles = card("Pym Particles") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target creature gains vigilance until end of turn and can't be blocked this turn.\n" +
        "Draw a card."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.VIGILANCE, t),
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t),
            Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Eli Minaya"
        flavorText = "\"There's a whole different perspective from down here. I like it.\"\n" +
            "—The Wasp, Janet Van Dyne"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/928dfa54-1ead-4eff-a538-36cb94f05b78.jpg?1783902952"
    }
}
