package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Lozhan, Dragons' Legacy
 * {3}{U}{R}
 * Legendary Creature — Dragon Shaman
 * 4/2
 * Flying
 * Whenever you cast an Adventure or Dragon spell, Lozhan deals damage equal to that spell's mana
 * value to any target that isn't a commander.
 *
 * An Adventure spell is on the stack with only its Adventure characteristics (CR 715.3b), so its
 * type line carries the Adventure subtype and its mana value is the Adventure's cost — one subtype
 * filter covers both halves of "Adventure or Dragon", and a non-Dragon adventurer cast as its
 * creature matches neither.
 */
val LozhanDragonsLegacy = card("Lozhan, Dragons' Legacy") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Dragon Shaman"
    power = 4
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever you cast an Adventure or Dragon spell, Lozhan deals damage equal to that spell's " +
        "mana value to any target that isn't a commander."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withAnySubtype("Adventure", "Dragon"))
        val t = target(
            AnyTarget(
                descriptionOverride = "any target that isn't a commander",
                filter = GameObjectFilter.Any.withStatePredicate(StatePredicate.Not(StatePredicate.IsCommander)),
            )
        )
        effect = Effects.DealDamage(DynamicAmounts.triggeringSpellManaValue(), t)
        description = "Whenever you cast an Adventure or Dragon spell, Lozhan deals damage equal to " +
            "that spell's mana value to any target that isn't a commander."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "281"
        artist = "Rudy Siswanto"
        flavorText = "She channels myth and memory into waves of deadly arcane force."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe604232-19da-4331-a6f2-ddb004f1dd90.jpg?1783922690"
        ruling("2022-06-10", "If you cast a non-Dragon permanent spell that has an Adventure (rather than the Adventure itself) Lozhan's triggered ability will not trigger.")
        ruling("2022-06-10", "The mana value of an Adventure spell is the based on the mana cost of the Adventure, not the mana cost of the permanent card that has the Adventure.")
        ruling("2022-06-10", "\"Any target that isn't a commander\" includes any player, as well as any creature or planeswalker that isn't a commander.")
    }
}
