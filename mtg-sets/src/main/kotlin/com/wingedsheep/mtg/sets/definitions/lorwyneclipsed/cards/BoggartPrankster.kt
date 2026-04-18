package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Boggart Prankster
 * {1}{B}
 * Creature — Goblin Warrior
 * 1/3
 *
 * Whenever you attack, target attacking Goblin you control gets +1/+0 until end of turn.
 */
val BoggartPrankster = card("Boggart Prankster") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 3
    oracleText = "Whenever you attack, target attacking Goblin you control gets +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.YouAttack
        val goblin = target(
            "attacking Goblin you control",
            TargetCreature(filter = TargetFilter.AttackingCreature.withSubtype(Subtype.GOBLIN).youControl())
        )
        effect = Effects.ModifyStats(1, 0, goblin)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Karl Kopinski"
        flavorText = "\"Fetch my bow! And an umbrella!\"\n—Edren, kithkin farmer"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb228ea6-9235-4093-aea7-708e743b1b44.jpg?1767957110"
    }
}
