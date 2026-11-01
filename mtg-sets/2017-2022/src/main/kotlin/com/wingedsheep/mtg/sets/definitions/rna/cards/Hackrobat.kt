package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hackrobat
 * {1}{B}{R}
 * Creature — Human Rogue
 * 2/3
 * Spectacle {B}{R} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * {B}: This creature gains deathtouch until end of turn.
 * {R}: This creature gets +2/-2 until end of turn.
 */
val Hackrobat = card("Hackrobat") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Rogue"
    oracleText = "Spectacle {B}{R} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "{B}: This creature gains deathtouch until end of turn.\n" +
        "{R}: This creature gets +2/-2 until end of turn."
    power = 2
    toughness = 3

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{B}{R}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(2, -2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Simon Dominic"
        flavorText = "\"They all want to see me fall.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a41335c2-3d11-4f95-8d9f-66b04398c10b.jpg?1783933647"
    }
}
