package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Humbler of Mortals
 * {4}{G}{G}
 * Enchantment Creature — Elemental
 * 5/5
 *
 * Constellation — Whenever this creature or another enchantment you control enters, creatures you
 * control gain trample until end of turn.
 *
 * The Humbler is itself an enchantment, so one unbound watcher over enchantments you control
 * (`Triggers.a`) covers its own entry too (Eidolon of Blossoms' shape). The group is locked in on
 * resolution: creatures that enter afterwards this turn don't gain trample.
 */
val HumblerOfMortals = card("Humbler of Mortals") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Elemental"
    power = 5
    toughness = 5
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, creatures " +
        "you control gain trample until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Patterns.Group.grantKeywordToAll(Keyword.TRAMPLE, Filters.Group.creaturesYouControl)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "creatures you control gain trample until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Jason Felix"
        flavorText = "The anger of the gods multiplied in Nyx, and the land awoke in dark echoes of their rage."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f12149b9-b450-4ad6-ab5d-9a0eeb36997e.jpg?1783939413"
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
