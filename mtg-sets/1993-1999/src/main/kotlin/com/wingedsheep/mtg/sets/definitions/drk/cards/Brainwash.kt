package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackOrBlockUnlessPay
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Brainwash
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack unless its controller pays {3}.
 *
 * A tax, not a prohibition: the creature can always attack, it just costs {3} to declare — the
 * payment is part of declaring attackers (CR 508.1a), so it is charged once per combat and refunded
 * by nothing if the attack is later removed.
 *
 * `appliesToBlocking = false` is the whole difference from Myr Prototype's printed "can't attack or
 * block unless…": Brainwash leaves blocking free. The tax is charged for the *enchanted* creature,
 * which is why the combat-tax accounting reads the declared creature's attachments as well as its
 * own printed statics.
 */
val Brainwash = card("Brainwash") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't attack unless its controller pays {3}."
    auraTarget = Targets.Creature

    staticAbility {
        ability = CantAttackOrBlockUnlessPay(
            amount = DynamicAmount.Fixed(3),
            appliesToBlocking = false,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Pete Venters"
        flavorText = "\"They're not your friends; they despise you. I'm the only one you can count " +
            "on. Trust me.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6da4fb5a-0d24-4bee-b3f5-535ba9fe6850.jpg?1783947949"
    }
}
