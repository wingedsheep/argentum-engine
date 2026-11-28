package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hellrider
 * {2}{R}{R}
 * Creature — Devil
 * 3/3
 * Haste
 * Whenever a creature you control attacks, this creature deals 1 damage to the player or
 * planeswalker it's attacking.
 *
 * One trigger per attacking creature you control (Hellrider included). The damage goes to that
 * creature's own declared defender ([EffectTarget.AttackedBy]) — no target — so attackers split
 * across several players or planeswalkers each ping their own defender, and an attacker aimed at
 * a battle pings no one. Same composition as Raid Bombardment.
 */
val Hellrider = card("Hellrider") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Whenever a creature you control attacks, this creature deals 1 damage to the player or planeswalker it's attacking."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.DealDamage(1, EffectTarget.AttackedBy())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Svetlin Velinov"
        flavorText = "\"Behind every devil's mayhem lurks a demon's scheme.\"\n—Rem Karolus, Blade of the Inquisitors"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ec8d800-7f06-44e0-b22d-cdff0a9b153d.jpg?1783940817"

        ruling("2017-03-14", "Creatures you control may attack multiple players and/or planeswalkers. For each attacking creature, Hellrider will deal damage to the corresponding player or planeswalker.")
    }
}
