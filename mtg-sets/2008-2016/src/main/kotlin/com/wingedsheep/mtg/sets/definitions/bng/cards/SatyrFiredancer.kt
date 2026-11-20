package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Satyr Firedancer (BNG #108)
 * {1}{R} Enchantment Creature — Satyr, 1/1
 *
 * A damage observer on instant/sorcery spells you control (the Bloodfeather Phoenix shape). Because
 * the trigger is source-filtered, the damaged opponent is carried as the triggering player, so
 * "target creature that player controls" binds to exactly the opponent that spell damaged; one
 * trigger per damaged opponent. "That much" is the trigger's damage amount, dealt by this creature.
 */
val SatyrFiredancer = card("Satyr Firedancer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Satyr"
    oracleText = "Whenever an instant or sorcery spell you control deals damage to an opponent, " +
        "this creature deals that much damage to target creature that player controls."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.youControl()).dealsDamage(Recipient.Opponent)
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.targetPlayerControls(
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
        collectorNumber = "108"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fc6a735-cbf5-4af2-b15c-7ced82f0d9da.jpg?1783939542"
    }
}
