package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elder Spawn
 * {4}{U}{U}{U}
 * Creature — Spawn
 * 6/6
 * At the beginning of your upkeep, unless you sacrifice an Island, sacrifice this creature and it
 * deals 6 damage to you.
 * This creature can't be blocked by red creatures.
 *
 * The upkeep tax is [Effects.PayOrSuffer] with an Island sacrifice as the cost (Endless Wurm's
 * shape). Unlike Cosmic Horror, the damage is not conditioned on the sacrifice happening: the
 * Spawn deals 6 damage to you even if it's already gone, using last-known information for the
 * source.
 */
val ElderSpawn = card("Elder Spawn") {
    manaCost = "{4}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spawn"
    oracleText = "At the beginning of your upkeep, unless you sacrifice an Island, sacrifice this " +
        "creature and it deals 6 damage to you.\nThis creature can't be blocked by red creatures."
    power = 6
    toughness = 6

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.ISLAND)),
            suffer = SacrificeSelfEffect then
                Effects.DealDamage(6, EffectTarget.PlayerRef(Player.You)),
            consequenceDescription = "sacrifice Elder Spawn and it deals 6 damage to you",
        )
    }

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.RED))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99cc045e-01a8-4f14-a86d-0a67ec35d6b7.jpg?1783948077"
    }
}
