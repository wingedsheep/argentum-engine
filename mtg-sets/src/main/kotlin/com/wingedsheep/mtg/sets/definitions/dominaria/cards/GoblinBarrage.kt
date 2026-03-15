package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Goblin Barrage
 * {3}{R}
 * Sorcery
 * Kicker—Sacrifice an artifact or Goblin.
 * Goblin Barrage deals 4 damage to target creature. If this spell was kicked,
 * it also deals 4 damage to target player or planeswalker.
 */
val GoblinBarrage = card("Goblin Barrage") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"
    oracleText = "Kicker—Sacrifice an artifact or Goblin. (You may sacrifice an artifact or Goblin in addition to any other costs as you cast this spell.)\nGoblin Barrage deals 4 damage to target creature. If this spell was kicked, it also deals 4 damage to target player or planeswalker."

    keywordAbility(KeywordAbility.KickerWithAdditionalCost(
        AdditionalCost.SacrificePermanent(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature.withSubtype("Goblin")
        )
    ))

    spell {
        // Unkicked: 4 damage to target creature
        val creature = target("creature", Targets.Creature)
        effect = Effects.DealDamage(4, creature)

        // Kicked: 4 damage to target creature AND 4 damage to target player or planeswalker
        val kCreature = kickerTarget("creature", Targets.Creature)
        val kPlayerOrPw = kickerTarget("player or planeswalker", Targets.PlayerOrPlaneswalker)
        kickerEffect = Effects.Composite(
            Effects.DealDamage(4, kCreature),
            Effects.DealDamage(4, kPlayerOrPw)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4849db5d-cd41-49f6-acd5-697cdc8263f6.jpg?1562911270"
    }
}
