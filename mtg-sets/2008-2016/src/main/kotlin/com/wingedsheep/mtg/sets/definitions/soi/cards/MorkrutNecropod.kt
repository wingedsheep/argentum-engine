package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Morkrut Necropod — Shadows over Innistrad #125
 * {5}{B} · Creature — Slug Horror · 7/7
 *
 * Menace
 * Whenever this creature attacks or blocks, sacrifice another creature or land.
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (the Merfolk Skyscout shape):
 * attacking and blocking are separate events. The sacrifice is mandatory and chosen on resolution
 * from the controller's other creatures and lands; `excludeSource` keeps the Necropod — itself a
 * creature — off the list. With no other creature or land the effect does nothing (ruling).
 */
val MorkrutNecropod = card("Morkrut Necropod") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Slug Horror"
    power = 7
    toughness = 7
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks or blocks, sacrifice another creature or land."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.SacrificeOwn(GameObjectFilter.CreatureOrLand, excludeSource = true)
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.SacrificeOwn(GameObjectFilter.CreatureOrLand, excludeSource = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Alejandro Mirabal"
        flavorText = "\"There's not enough salt in all of Nephalia to wither that thing.\"\n—Ghoulcaller Enslow"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f726f215-d0a5-4e2c-b874-9dc209479993.jpg?1783937769"
        ruling(
            "2016-04-08",
            "When Morkrut Necropod attacks, its trigger doesn't resolve until after all attackers have " +
                "been chosen. The same is true for when it blocks. This means that another creature, such " +
                "as one enchanted with Invocation of Saint Traft, can also attack or block and then be " +
                "sacrificed. Sacrificing a blocking creature this way doesn't cause the creature it " +
                "blocked to become unblocked."
        )
        ruling(
            "2016-04-08",
            "In the unusual situation that you control no other creatures or lands, the last ability " +
                "won't do anything. Morkrut Necropod can attack or block with no penalty."
        )
    }
}
