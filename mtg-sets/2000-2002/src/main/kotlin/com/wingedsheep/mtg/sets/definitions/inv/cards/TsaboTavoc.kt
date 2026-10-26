package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.Recipient

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

    keywordAbility(KeywordAbility.protectionFromSupertype("Legendary", "Creature"))

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    activatedAbility {
        cost = Costs.Tap
        val legendaryCreature = target(TargetFilter(baseFilter = GameObjectFilter.Creature.legendary()))
        effect = Effects.Destroy(legendaryCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "281"
        artist = "Michael Sutfin"
        flavorText = "\"I might almost pity my enemies—if it wasn't so amusing to watch them die.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccbe2539-7a7c-468b-a270-7ca1bdcccb1e.jpg?1783945654"
    }
}
