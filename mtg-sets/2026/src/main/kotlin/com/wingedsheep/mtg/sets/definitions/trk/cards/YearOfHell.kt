package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Year of Hell — Star Trek #257
 * {1}{U}{R} · Enchantment — Saga
 *
 * I, II, III — Each player draws a card. This Saga deals 2 damage to each player.
 *
 * The Saga is the damage source: chapter III resolves before the state-based sacrifice.
 */
val YearOfHell = card("Year of Hell") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II, III — Each player draws a card. This Saga deals 2 damage to each player."

    for (chapter in 1..3) {
        sagaChapter(chapter) {
            effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each)) then
                Effects.DealDamage(2, EffectTarget.PlayerRef(Player.Each))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Jonathan Bartlett"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4ab931e9-fb07-4da2-a0d9-acfec2ac9e19.jpg?1792753960"
    }
}
