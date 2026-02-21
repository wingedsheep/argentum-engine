package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Commando Raid
 * {2}{R}
 * Instant
 * Until end of turn, target creature you control gains "Whenever this creature deals
 * combat damage to a player, you may have it deal damage equal to its power to target
 * creature that player controls."
 */
val CommandoRaid = card("Commando Raid") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature you control gains \"Whenever this creature deals combat damage to a player, you may have it deal damage equal to its power to target creature that player controls.\""

    spell {
        val t = target("target", Targets.CreatureYouControl)
        effect = GrantTriggeredAbilityUntilEndOfTurnEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToPlayer.event,
                binding = Triggers.DealsCombatDamageToPlayer.binding,
                effect = MayEffect(DealDamageEffect(DynamicAmount.SourcePower, EffectTarget.ContextTarget(0))),
                targetRequirement = Targets.CreatureOpponentControls
            ),
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Ron Spencer"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/b/b/bb237330-ac2e-411d-836c-6628f96f3262.jpg?1562936979"
    }
}
