package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stormfist Crusader
 * {B}{R}
 * Creature — Human Knight
 * 2/2
 * Menace
 * At the beginning of your upkeep, each player draws a card and loses 1 life.
 *
 * Every player draws first, then every player loses 1 life — the order the Two-Headed Giant
 * ruling describes ("after each player on that team has drawn a card").
 */
val StormfistCrusader = card("Stormfist Crusader") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Knight"
    oracleText = "Menace\n" +
        "At the beginning of your upkeep, each player draws a card and loses 1 life."
    power = 2
    toughness = 2

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each)) then
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Chris Rallis"
        flavorText = "\"As she reached the pinnacle, lightning flashed and her eyes blazed with newfound power.\"\n—*Legend of the Gilded Knights*"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27425f2e-e0b2-489d-877d-8257d2026bfd.jpg?1783932593"
        ruling(
            "2019-10-04",
            "In a Two-Headed Giant game, Stormfist Crusader's triggered ability causes each team to lose 1 life " +
                "twice after each player on that team has drawn a card."
        )
    }
}
