package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sygg, Wanderwine Wisdom // Sygg, Wanderbrine Shield
 * {1}{U}
 * Legendary Creature — Merfolk Wizard // Legendary Creature — Merfolk Rogue (transform)
 * 2/2 // 2/2
 *
 * Front — Sygg, Wanderwine Wisdom
 *   Sygg can't be blocked.
 *   Whenever this creature enters or transforms into Sygg, Wanderwine Wisdom, target creature
 *   gains "Whenever this creature deals combat damage to a player or planeswalker, draw a card"
 *   until end of turn.
 *   At the beginning of your first main phase, you may pay {W}. If you do, transform Sygg.
 *
 * Back — Sygg, Wanderbrine Shield
 *   Sygg can't be blocked.
 *   Whenever this creature transforms into Sygg, Wanderbrine Shield, target creature you control
 *   gains protection from each color until your next turn.
 *   At the beginning of your first main phase, you may pay {U}. If you do, transform Sygg.
 */

private val drawOnCombatDamageSpec = Triggers.dealsDamage(
    damageType = DamageType.Combat,
    recipient = RecipientFilter.AnyPlayerOrPlaneswalker,
)

private val drawOnCombatDamage = TriggeredAbility.create(
    trigger = drawOnCombatDamageSpec.event,
    binding = drawOnCombatDamageSpec.binding,
    effect = Effects.DrawCards(1)
)

// Protection from each color until your next turn — compose five keyword grants so each
// color is independently tracked by the floating-effect cleanup system.
private fun protectionFromEachColor(target: EffectTarget): Effect = Effects.Composite(
    Color.entries.map { color ->
        GrantKeywordEffect(
            keyword = "PROTECTION_FROM_${color.name}",
            target = target,
            duration = Duration.UntilYourNextTurn
        )
    }
)

private val SyggWanderbrineShield = card("Sygg, Wanderbrine Shield") {
    manaCost = ""
    typeLine = "Legendary Creature — Merfolk Rogue"
    power = 2
    toughness = 2
    oracleText = "Sygg can't be blocked.\n" +
        "Whenever this creature transforms into Sygg, Wanderbrine Shield, target creature you " +
        "control gains protection from each color until your next turn.\n" +
        "At the beginning of your first main phase, you may pay {U}. If you do, transform Sygg."

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.TransformsToBack
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = protectionFromEachColor(t)
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{U}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/back/7/0/70adc870-f0db-4d4b-863b-673c2c258751.jpg?1759144840"
    }
}

private val SyggWanderwineWisdomFront = card("Sygg, Wanderwine Wisdom") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "Sygg can't be blocked.\n" +
        "Whenever this creature enters or transforms into Sygg, Wanderwine Wisdom, target " +
        "creature gains \"Whenever this creature deals combat damage to a player or " +
        "planeswalker, draw a card\" until end of turn.\n" +
        "At the beginning of your first main phase, you may pay {W}. If you do, transform Sygg."

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature", Targets.Creature)
        effect = GrantTriggeredAbilityEffect(
            ability = drawOnCombatDamage,
            target = t,
            duration = Duration.EndOfTurn
        )
    }

    triggeredAbility {
        trigger = Triggers.TransformsToFront
        val t = target("target creature", Targets.Creature)
        effect = GrantTriggeredAbilityEffect(
            ability = drawOnCombatDamage,
            target = t,
            duration = Duration.EndOfTurn
        )
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{W}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70adc870-f0db-4d4b-863b-673c2c258751.jpg?1759144840"
    }
}

val SyggWanderwineWisdom: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SyggWanderwineWisdomFront,
    backFace = SyggWanderbrineShield
)
