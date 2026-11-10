package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pyromancer's Assault
 * {3}{R}
 * Enchantment
 * Whenever you cast your second spell each turn, this enchantment deals 2 damage to any target.
 */
val PyromancersAssault = card("Pyromancer's Assault") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast your second spell each turn, this enchantment deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.castsNth(2)
        val any = target(Targets.Any)
        effect = Effects.DealDamage(2, any)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Todd Lockwood"
        flavorText = "Never ask a pyromancer if that's the best she's got."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2c8dad1-278c-4ced-8124-5593b140d361.jpg"
    }
}
