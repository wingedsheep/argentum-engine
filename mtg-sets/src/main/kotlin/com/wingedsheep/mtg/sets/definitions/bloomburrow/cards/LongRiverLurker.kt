package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWardToGroup
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Long River Lurker
 * {2}{U}
 * Creature — Frog Scout
 * 2/3
 * Ward {1}
 * Other Frogs you control have ward {1}.
 * When this creature enters, target creature you control can't be blocked this turn.
 * Whenever that creature deals combat damage this turn, you may exile it. If you do,
 * return it to the battlefield under its owner's control.
 *
 * The ETB grants "can't be blocked this turn" to the target. The combat damage
 * exile-return delayed trigger is modeled as part of the ETB composite effect but
 * requires the target to actually deal combat damage first (which the "can't be blocked"
 * part facilitates).
 */
val LongRiverLurker = card("Long River Lurker") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Frog Scout"
    power = 2
    toughness = 3
    oracleText = "Ward {1}\nOther Frogs you control have ward {1}.\nWhen this creature enters, target creature you control can't be blocked this turn. Whenever that creature deals combat damage this turn, you may exile it. If you do, return it to the battlefield under its owner's control."

    keywordAbility(KeywordAbility.ward("{1}"))

    // Other Frogs you control have ward {1}
    staticAbility {
        ability = GrantWardToGroup(
            manaCost = "{1}",
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Frog").youControl()).other()
        )
    }

    // ETB: target creature you control can't be blocked this turn, and whenever that
    // creature deals combat damage this turn, you may exile it and return it.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = CompositeEffect(listOf(
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature),
            CreateDelayedTriggerEffect(
                effect = MayEffect(
                    effect = CompositeEffect(listOf(
                        MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.EXILE),
                        MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD)
                    )),
                    description_override = "You may exile that creature. If you do, return it to the battlefield under its owner's control."
                ),
                trigger = Triggers.DealsCombatDamage,
                watchedTarget = EffectTarget.ContextTarget(0),
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c267719-cd03-4003-b281-e732d5e42a1e.jpg?1721426137"
        ruling("2024-07-26", "Once the exiled permanent returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2024-07-26", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
