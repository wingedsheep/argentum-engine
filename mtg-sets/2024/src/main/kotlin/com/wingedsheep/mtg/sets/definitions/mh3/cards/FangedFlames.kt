package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fanged Flames — Modern Horizons 3 #118
 * {1}{R} · Sorcery
 *
 * Devoid (This card has no color.)
 * Fanged Flames deals 4 damage to target creature or planeswalker. If that creature or
 * planeswalker would die this turn, exile it instead.
 *
 * Scorching Dragonfire's shape at 4 damage and sorcery speed, with Devoid (derived colourless by
 * `CardDefinition.colors`; colour identity stays red). The exile-on-death mark goes on before the
 * damage so the death this very spell causes is exiled.
 */
val FangedFlames = card("Fanged Flames") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Fanged Flames deals 4 damage to target creature or planeswalker. " +
        "If that creature or planeswalker would die this turn, exile it instead."

    keywords(Keyword.DEVOID)

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.MarkExileOnDeath(t) then Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Campbell White"
        flavorText = "With Dorble's natural immunity to venom, he'd always taken certain comfort in the " +
            "knowledge that he wouldn't die of snakebite."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcfac301-55db-49a7-9a0d-918c907703da.jpg?1783911273"
    }
}
