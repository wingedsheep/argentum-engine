package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Monstrous Onslaught — Aether Revolt #116 (canonical printing)
 * {3}{G}{G} · Instant
 *
 * Monstrous Onslaught deals X damage divided as you choose among any number of target creatures,
 * where X is the greatest power among creatures you control as you cast this spell.
 *
 * X is frozen as the spell is cast with `captureAtCast` (CR 601.2i), and the division is announced
 * at cast (CR 601.2d) against that captured amount, so a later change to your creatures' power
 * doesn't move X, and a target that becomes illegal loses its share (per the rulings). Each target
 * must be dealt at least 1, so the target count is capped by the same greatest power.
 */
val MonstrousOnslaught = card("Monstrous Onslaught") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Monstrous Onslaught deals X damage divided as you choose among any number of target " +
        "creatures, where X is the greatest power among creatures you control as you cast this spell."

    spell {
        val greatestPower = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        captureAtCast("x", greatestPower)
        targets(
            TargetFilter.Creature,
            minCount = 0,
            unlimited = true,
            dynamicMaxCount = greatestPower,
        )
        effect = Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.capturedAtCast("x"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Slawomir Maniak"
        flavorText = "\"Regards from Peema.\"\n—Rishkar"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2aebf95d-a231-402a-8888-9c50889a9556.jpg?1783936741"
        ruling("2020-11-10", "You divide the damage among the target creatures as you cast Monstrous Onslaught. Each target must be assigned at least 1 damage.")
        ruling("2020-11-10", "The value of X is determined at the time you divide damage. It won't change later, even if the greatest power among creatures you control changes.")
        ruling("2020-11-10", "If some of the targets become illegal, the original division of damage still applies. The damage that was divided among the illegal targets simply isn't dealt; it isn't re-assigned to remaining legal targets.")
        ruling("2020-11-10", "The damage is dealt by Monstrous Onslaught, not by the creature with the greatest power among creatures you control.")
    }
}
