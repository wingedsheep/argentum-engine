package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Forgeborn Oreads
 * {2}{R}{R}
 * Enchantment Creature — Nymph
 * 4/2
 *
 * Constellation — Whenever this creature or another enchantment you control enters, this creature
 * deals 1 damage to any target.
 *
 * The Oreads is itself an enchantment, so one unbound watcher over enchantments you control
 * (`Triggers.a`) covers its own entry too (Eidolon of Blossoms' shape).
 */
val ForgebornOreads = card("Forgeborn Oreads") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Nymph"
    power = 4
    toughness = 2
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, this " +
        "creature deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "this creature deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Ryan Yee"
        flavorText = "Purphoros shaped the oreads out of stray coals from his forge."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b80038ed-4206-4e44-943e-f332e4ce2f14.jpg?1783939424"
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
