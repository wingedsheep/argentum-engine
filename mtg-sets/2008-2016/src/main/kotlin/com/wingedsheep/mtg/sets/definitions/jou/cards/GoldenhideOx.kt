package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Goldenhide Ox
 * {5}{G}
 * Enchantment Creature — Ox
 * 5/4
 *
 * Constellation — Whenever this creature or another enchantment you control enters, target
 * creature must be blocked this turn if able.
 *
 * The Ox is itself an enchantment, so one watcher over enchantments you control covers its own
 * entry too (Harvestguard Alseids' shape). "Must be blocked if able" asks for at least one blocker,
 * not every able blocker, so [Effects.MustBeBlocked] runs with `allCreatures = false`.
 */
val GoldenhideOx = card("Goldenhide Ox") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Ox"
    power = 5
    toughness = 4
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, target " +
        "creature must be blocked this turn if able."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.MustBeBlocked(creature, allCreatures = false)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "target creature must be blocked this turn if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Jack Wang"
        flavorText = "The temptation to take the ox's golden hide is as irresistible as it is fatal."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d107c72f-483a-4af9-b86f-681669d99c46.jpg?1783939414"
        ruling(
            "2014-04-26",
            "The defending player must assign at least one blocker to the target creature during the " +
                "declare blockers step if that player controls any creatures that could block it."
        )
        ruling(
            "2014-04-26",
            "The constellation ability doesn't force any specific creature to block the target creature."
        )
        ruling(
            "2014-04-26",
            "If multiple attacking creatures must be blocked if able, the defending player must assign at " +
                "least one blocker to each of them if possible. For example, if two such creatures were " +
                "attacking and there were two potential blockers, they couldn't both be assigned to block " +
                "the same attacker."
        )
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
