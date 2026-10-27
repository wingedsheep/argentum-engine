package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Koma's Faithful
 * {2}{B}
 * Creature — Elf Cleric
 * 3/1
 * Lifelink
 * When this creature dies, each player mills three cards.
 */
val KomasFaithful = card("Koma's Faithful") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Cleric"
    power = 3
    toughness = 1
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "When this creature dies, each player mills three cards. " +
        "(They each put the top three cards of their library into their graveyard.)"

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature dies, each player mills three cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Caroline Gariba"
        flavorText = "\"Great Serpent, hear my song! We wait, our faith unshaken, for your glorious return.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af0f1a90-af64-4122-9c1a-954edf8fa240.jpg?1783928247"
    }
}
