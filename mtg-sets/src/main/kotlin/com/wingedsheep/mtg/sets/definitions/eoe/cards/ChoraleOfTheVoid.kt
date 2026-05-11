package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chorale of the Void
 * {3}{B}
 * Enchantment — Aura
 * Enchant creature you control
 * Whenever enchanted creature attacks, put target creature card from defending player's graveyard
 * onto the battlefield under your control tapped and attacking.
 * Void — At the beginning of your end step, sacrifice this Aura unless a nonland permanent left
 * the battlefield this turn or a spell was warped this turn.
 */
val ChoraleOfTheVoid = card("Chorale of the Void") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "Whenever enchanted creature attacks, put target creature card from defending player's graveyard onto the battlefield under your control tapped and attacking.\n" +
        "Void — At the beginning of your end step, sacrifice this Aura unless a nonland permanent left the battlefield this turn or a spell was warped this turn."

    auraTarget = Targets.CreatureYouControl

    triggeredAbility {
        trigger = Triggers.EnchantedCreatureAttacks
        val creature = target(
            "creature card from defending player's graveyard",
            TargetObject(filter = TargetFilter.CreatureInGraveyard.ownedByOpponent())
        )
        effect = MoveToZoneEffect(
            target = creature,
            destination = Zone.BATTLEFIELD,
            placement = ZonePlacement.TappedAndAttacking,
            controllerOverride = EffectTarget.Controller
        )
        description = "Put target creature card from defending player's graveyard onto the battlefield under your control tapped and attacking."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.Not(Conditions.Void)
        effect = SacrificeSelfEffect
        description = "Sacrifice this Aura unless a nonland permanent left the battlefield this turn or a spell was warped this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7389fe88-f6ff-4497-a037-9ca283fb89e3.jpg?1752946923"
        ruling("2025-07-25", "You choose the player, planeswalker, or battle the creature you put onto the battlefield is attacking. It doesn't have to be the same player, planeswalker, or battle that the enchanted creature or any other attacking creatures are attacking.")
        ruling("2025-07-25", "Although the creature you put onto the battlefield is attacking, it was never declared as an attacking creature. Abilities that trigger whenever a creature attacks won't trigger when that creature enters attacking.")
    }
}
