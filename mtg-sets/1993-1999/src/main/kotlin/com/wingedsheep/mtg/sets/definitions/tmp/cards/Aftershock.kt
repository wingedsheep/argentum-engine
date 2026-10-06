package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aftershock
 * {2}{R}{R}
 * Sorcery
 * Destroy target artifact, creature, or land. Aftershock deals 3 damage to you.
 */
val Aftershock = card("Aftershock") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, creature, or land. Aftershock deals 3 damage to you."

    spell {
        val t = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land))
        effect = Effects.Destroy(t) then
            Effects.DealDamage(3, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Hannibal King"
        flavorText = "\"Every act of destruction has a repercussion.\"\n—Karn, silver golem"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c91a26b2-03f8-43f0-a3a4-ff6c5a3690c4.jpg?1783946633"
    }
}
