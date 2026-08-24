package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Close Quarters
 * {2}{R}{R}
 * Enchantment
 *
 * Whenever a creature you control becomes blocked, this enchantment deals 1 damage to any target.
 *
 * The Gustcloak Savior shape: the *filtered, ANY-binding* [Triggers.becomesBlocked] factory.
 * `Triggers.BecomesBlocked` is its SELF-binding sibling and is wrong here — the enchantment is
 * never itself the blocked creature, so a SELF binding would simply never fire.
 *
 * The trigger fires once per creature that becomes blocked (CR 509.1h), independent of how many
 * blockers were assigned to it, and the damage target is chosen when the ability goes on the
 * stack rather than at resolution.
 */
val CloseQuarters = card("Close Quarters") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control becomes blocked, this enchantment deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.becomesBlocked(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY,
        )
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Ron Spencer"
        flavorText = "The Mercadians' ineptitude in close combat sometimes accidentally pays off."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b9131c7-4e46-4c01-80b3-a6b055439346.jpg"
    }
}
