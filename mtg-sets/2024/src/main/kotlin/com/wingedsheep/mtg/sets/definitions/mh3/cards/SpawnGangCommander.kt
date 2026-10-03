package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Spawn-Gang Commander — Modern Horizons 3 #140 (uncommon)
 * {3}{R}{R} · Creature — Eldrazi Goblin · 2/2
 *
 * Devoid
 * When you cast this spell, create three 0/1 colorless Eldrazi Spawn creature tokens with
 * "Sacrifice this token: Add {C}."
 * {1}{C}, Sacrifice an Eldrazi: This creature deals 2 damage to any target.
 *
 * The sacrificed Eldrazi may be the Commander itself (it is an Eldrazi); the damage then comes
 * from it as last known information.
 */
val SpawnGangCommander = card("Spawn-Gang Commander") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Goblin"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell, create three 0/1 colorless Eldrazi Spawn creature tokens with " +
        "\"Sacrifice this token: Add {C}.\"\n" +
        "{1}{C}, Sacrifice an Eldrazi: This creature deals 2 damage to any target."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CreateEldraziSpawn(3)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{C}"),
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Eldrazi")),
        )
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecac9885-6f4e-4413-90dc-c8b44f883357.jpg?1783911265"
    }
}
