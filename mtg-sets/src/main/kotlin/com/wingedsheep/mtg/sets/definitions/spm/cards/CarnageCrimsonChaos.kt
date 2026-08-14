package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Carnage, Crimson Chaos — Marvel's Spider-Man #125
 * {2}{B}{R} · Legendary Creature — Symbiote Villain · 4/3
 *
 * Trample
 * When Carnage enters, return target creature card with mana value 3 or less from your graveyard
 * to the battlefield. It gains "This creature attacks each combat if able" and "When this creature
 * deals combat damage to a player, sacrifice it."
 * Mayhem {B}{R}
 *
 * The two granted abilities use `GrantStaticAbilityEffect(MustAttack())` and
 * `GrantTriggeredAbilityEffect(... SacrificeSelfEffect)` with `Duration.Permanent`, keyed to the
 * reanimated creature.
 */
val CarnageCrimsonChaos = card("Carnage, Crimson Chaos") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Symbiote Villain"
    power = 4
    toughness = 3
    oracleText = "Trample\nWhen Carnage enters, return target creature card with mana value 3 or less from your graveyard to the battlefield. It gains \"This creature attacks each combat if able\" and \"When this creature deals combat damage to a player, sacrifice it.\"\nMayhem {B}{R}"

    keywords(Keyword.TRAMPLE)
    mayhem("{B}{R}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "target creature card",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3))
        )
        effect = Effects.Composite(
            Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
            GrantStaticAbilityEffect(MustAttack(), creature, Duration.Permanent),
            GrantTriggeredAbilityEffect(
                ability = TriggeredAbility.create(
                    trigger = Triggers.DealsCombatDamageToPlayer.event,
                    binding = Triggers.DealsCombatDamageToPlayer.binding,
                    effect = SacrificeSelfEffect
                ),
                target = creature,
                duration = Duration.Permanent
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Lordigan"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/930befba-6068-493e-baa2-e9371cd99e93.jpg?1783905319"
    }
}
