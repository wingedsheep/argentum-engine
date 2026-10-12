package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Corrupt
 * {5}{B}
 * Sorcery
 * Corrupt deals damage to any target equal to the number of Swamps you control. You gain life
 * equal to the damage dealt this way.
 *
 * Swamps are counted on resolution (any land with the Swamp subtype). The life gain reads the
 * damage actually dealt (`damageDealtVariable`), so prevention reduces it and it is not capped by
 * the target's toughness or life total.
 */
val Corrupt = card("Corrupt") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Corrupt deals damage to any target equal to the number of Swamps you control. " +
        "You gain life equal to the damage dealt this way."

    spell {
        val anyTarget = target(Targets.Any)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Swamp")).count(),
                    anyTarget,
                    damageDealtVariable = it,
                )
            }
            run(Effects.GainLife(dealt.amount))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Vincent Evans"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32e1c65c-ced6-484d-b3b4-db913c6bf84b.jpg?1783946347"
        flavorText = "Yawgmoth brushed Urza's mind, and Urza's world convulsed."
        ruling("2022-10-14", "Corrupt counts any land you control with the subtype Swamp, not just ones named Swamp.")
        ruling("2010-08-15", "The life you gain is equal to the damage dealt by Corrupt, not necessarily to the number of Swamps you control (if some or all of the damage is prevented, for example).")
        ruling("2010-08-15", "The amount of damage dealt to a creature is not bounded by its toughness, and the amount of damage dealt to a player is not bounded by that player's life total. For example, if Corrupt deals 6 damage to a 2/2 creature, you'll gain 6 life.")
        ruling("2004-10-04", "The swamps are counted on resolution.")
    }
}
