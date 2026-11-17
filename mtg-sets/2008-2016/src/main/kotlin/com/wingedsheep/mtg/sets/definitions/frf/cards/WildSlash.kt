package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wild Slash
 * {R}
 * Instant
 * Ferocious — If you control a creature with power 4 or greater, damage can't be prevented this turn.
 * Wild Slash deals 2 damage to any target.
 *
 * The ferocious check is made once, as the spell resolves, and comes before the damage, so the
 * turn-scoped "can't be prevented" covers Wild Slash's own 2 damage too (Stomp's shape, gated).
 */
val WildSlash = card("Wild Slash") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Ferocious — If you control a creature with power 4 or greater, damage can't be prevented this turn.\n" +
        "Wild Slash deals 2 damage to any target."

    spell {
        val t = target(Targets.Any)
        effect = Effects.If(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            then = Effects.DamageCantBePreventedThisTurn()
        ) then Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Raymond Swanland"
        flavorText = "Never mistake deception for cleverness."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6975490f-7679-48b3-ba34-04dec97a29c2.jpg?1783938683"
        ruling(
            "2014-11-24",
            "Wild Slash's ferocious ability applies to all damage that would be dealt that turn, including the damage Wild Slash deals.",
        )
        ruling(
            "2014-11-24",
            "If you control a creature with power 4 or greater as Wild Slash resolves, the ferocious ability will apply. Damage dealt that turn can't be prevented, even if you no longer control a creature with power 4 or greater as that damage would be dealt.",
        )
    }
}
