package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Focus Fire
 * {W}
 * Instant
 *
 * Focus Fire deals X damage to target attacking or blocking creature, where X is
 * 2 plus the number of creatures and/or Spacecraft you control.
 *
 * A Spacecraft you control that's also a creature counts only once toward X — a
 * single count over the "creature OR Spacecraft" filter matches each permanent at
 * most once. X is evaluated only as Focus Fire resolves (standard for DynamicAmount).
 */
val FocusFire = card("Focus Fire") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Focus Fire deals X damage to target attacking or blocking creature, where X is 2 plus the number of creatures and/or Spacecraft you control."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.AttackingOrBlockingCreature))
        effect = Effects.DealDamage(
            DynamicAmount.Add(
                DynamicAmount.Fixed(2),
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature or GameObjectFilter.Permanent.withSubtype("Spacecraft")
                ).count()
            ),
            t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Borja Pindado"
        flavorText = "\"We are the arrows of Sunsolde's Quiver.\"\n—*Declaration of Fleet Purpose*, Axiom 2"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9ddfcbc-0f84-4315-aaa3-ca54ff64d7de.jpg?1752946622"
        ruling("2025-07-25", "The value of X is calculated only once, as Focus Fire resolves.")
        ruling("2025-07-25", "A Spacecraft you control that's also a creature counts only once toward the value of X.")
    }
}
