package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rockslide Sorcerer — Zendikar Rising #154
 * {3}{R} · Creature — Human Wizard Sorcerer · 3/3
 *
 * Whenever you cast an instant, sorcery, or Wizard spell, this creature deals 1 damage to any target.
 */
val RockslideSorcerer = card("Rockslide Sorcerer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard Sorcerer"
    oracleText = "Whenever you cast an instant, sorcery, or Wizard spell, this creature deals 1 damage to any target."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery or GameObjectFilter.Any.withSubtype("Wizard")
        )
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
        description = "Whenever you cast an instant, sorcery, or Wizard spell, this creature deals 1 " +
            "damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Daarken"
        flavorText = "\"You can cower from the crushing shards or embrace the danger of Akoum. I've made my choice.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e105c546-ce4f-49c7-97ba-ea5fbe70df82.jpg?1783929354"
        ruling("2020-09-25", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger, but after targets have been chosen for that spell (if it has any targets). The ability resolves even if that spell is countered.")
        ruling("2020-09-25", "A Wizard spell is one with the creature type Wizard. Spells that are Wizard-themed (such as Relic Amulet) aren't Wizard spells.")
    }
}
