package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Incinerator — Core Set 2021 #136
 * {5}{R} · Creature — Elemental · 6/6
 *
 * The cost reduction reads `TurnTracker.NONCOMBAT_DAMAGE_RECEIVED` over each opponent — all
 * noncombat damage dealt to them this turn, from any source and including opponents who have since
 * lost the game (rulings); it reduces generic mana only. The trigger is the source-you-control
 * noncombat damage observer, where "that player" is the damaged opponent (`Player.TriggeringPlayer`).
 */
val ChandrasIncinerator = card("Chandra's Incinerator") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 6
    oracleText = "This spell costs {X} less to cast, where X is the total amount of noncombat damage dealt to your " +
        "opponents this turn.\n" +
        "Trample\n" +
        "Whenever a source you control deals noncombat damage to an opponent, this creature deals that much " +
        "damage to target creature or planeswalker that player controls."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.noncombatDamageReceivedThisTurn(Player.EachOpponent)),
            ),
        )
    }

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl()).dealsDamage(Recipient.Opponent, damageType = DamageType.NonCombat)
        val t = target(
            TargetFilter(
                GameObjectFilter.CreatureOrPlaneswalker.targetPlayerControls(
                    EffectTarget.PlayerRef(Player.TriggeringPlayer),
                ),
            ),
        )
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = t,
            damageSource = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed875705-b7b6-4464-b16f-61629ffed04f.jpg?1783930694"
    }
}
