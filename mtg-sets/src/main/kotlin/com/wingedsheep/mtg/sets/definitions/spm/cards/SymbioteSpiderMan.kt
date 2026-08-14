package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Symbiote Spider-Man
 * {2}{U/B}
 * Legendary Creature — Symbiote Spider Hero
 * 2/4
 *
 * Whenever this creature deals combat damage to a player, look at that many cards from the top of
 * your library. Put one of them into your hand and the rest into your graveyard.
 * Find New Host — {2}{U/B}, Exile this card from your graveyard: Put a +1/+1 counter on target
 * creature you control. It gains this card's other abilities. Activate only as a sorcery.
 *
 * "That many" is the combat damage dealt: `DynamicAmount.ContextProperty(TRIGGER_DAMAGE_AMOUNT)`
 * feeds [Patterns.Library.lookAtTopAndKeep] (look at N, keep 1 to hand, rest to graveyard) — the
 * Sultai Soothsayer / The Key to the Vault dig pipeline.
 *
 * "It gains this card's other abilities" grants the *same* combat-damage dig trigger
 * ([symbioteCombatDamageTrigger]) to the target via [GrantTriggeredAbilityEffect] with
 * [Duration.Permanent] — the Avatar earthbend/firebending grant-your-own-printed-ability pattern.
 * The only "other" printed ability is that trigger; the Find New Host activated ability being
 * resolved is itself excluded, and as a graveyard-exile ability it could not function on a
 * battlefield creature anyway.
 */
private fun symbioteDigEffect(): Effect = Patterns.Library.lookAtTopAndKeep(
    count = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
    keepCount = DynamicAmount.Fixed(1)
)

private const val DIG_DESCRIPTION =
    "Whenever this creature deals combat damage to a player, look at that many cards from the top " +
        "of your library. Put one of them into your hand and the rest into your graveyard."

private fun symbioteCombatDamageTrigger(): TriggeredAbility = TriggeredAbility.create(
    trigger = Triggers.DealsCombatDamageToPlayer.event,
    binding = Triggers.DealsCombatDamageToPlayer.binding,
    effect = symbioteDigEffect(),
    descriptionOverride = DIG_DESCRIPTION,
)

val SymbioteSpiderMan = card("Symbiote Spider-Man") {
    manaCost = "{2}{U/B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Symbiote Spider Hero"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature deals combat damage to a player, look at that many cards " +
        "from the top of your library. Put one of them into your hand and the rest into your " +
        "graveyard.\n" +
        "Find New Host — {2}{U/B}, Exile this card from your graveyard: Put a +1/+1 counter on " +
        "target creature you control. It gains this card's other abilities. Activate only as a " +
        "sorcery."

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = symbioteDigEffect()
        description = DIG_DESCRIPTION
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U/B}"), Costs.ExileSelf)
        val creature = target("target", TargetCreature(filter = TargetFilter.CreatureYouControl))
        effect = Effects.Composite(
            AddCountersEffect(counterType = Counters.PLUS_ONE_PLUS_ONE, count = 1, target = creature),
            GrantTriggeredAbilityEffect(
                ability = symbioteCombatDamageTrigger(),
                target = creature,
                duration = Duration.Permanent,
            ),
        )
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
        description = "Find New Host — {2}{U/B}, Exile this card from your graveyard: Put a +1/+1 " +
            "counter on target creature you control. It gains this card's other abilities. " +
            "Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Paolo Rivera"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a21c0ff-b51a-4946-9737-7872a7eef97b.jpg?1783905310"
    }
}
