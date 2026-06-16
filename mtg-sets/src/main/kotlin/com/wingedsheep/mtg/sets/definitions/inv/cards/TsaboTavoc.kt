package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tsabo Tavoc
 * {4}{B}{R}{R}
 * Legendary Creature — Horror
 * 6/4
 * Protection from legendary creatures
 * Whenever Tsabo Tavoc deals combat damage to a player, that player sacrifices a creature.
 * {T}: Destroy target legendary creature.
 */
val TsaboTavoc = card("Tsabo Tavoc") {
    manaCost = "{4}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Horror"
    power = 6
    toughness = 4
    oracleText = "Protection from legendary creatures\n" +
        "Whenever Tsabo Tavoc deals combat damage to a player, that player sacrifices a creature.\n" +
        "{T}: Destroy target legendary creature."

    keywordAbility(KeywordAbility.protectionFromSupertype("Legendary"))

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = ForceSacrificeEffect(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    activatedAbility {
        cost = Costs.Tap
        val legendaryCreature = target(
            "target legendary creature",
            TargetObject(filter = TargetFilter(baseFilter = GameObjectFilter.Creature.legendary())),
        )
        effect = Effects.Destroy(legendaryCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "280"
    }
}
