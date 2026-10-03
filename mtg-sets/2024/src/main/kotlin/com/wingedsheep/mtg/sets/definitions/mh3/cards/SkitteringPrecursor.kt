package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skittering Precursor — Modern Horizons 3 #137 (uncommon)
 * {2}{R} · Creature — Eldrazi Drone · 3/3
 *
 * Devoid
 * Menace
 * Whenever you sacrifice a nontoken permanent, create a 0/1 colorless Eldrazi Spawn creature token
 * with "Sacrifice this token: Add {C}."
 *
 * "a nontoken permanent" is the per-permanent template (CR 603.2c) with the ANY binding.
 */
val SkitteringPrecursor = card("Skittering Precursor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "Menace\n" +
        "Whenever you sacrifice a nontoken permanent, create a 0/1 colorless Eldrazi Spawn creature " +
        "token with \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Permanent.nontoken())
        effect = Effects.CreateEldraziSpawn()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Tuan Duong Chu"
        flavorText = "It pushes ahead, eliminating lookouts and tactically sowing spawn."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3a3b943-7b38-4316-87d9-15e0c08abea5.jpg?1783911266"
    }
}
