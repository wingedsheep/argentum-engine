package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slith Bloodletter — Mirrodin #77
 * {B}{B} · Creature — Slith · 1/1
 *
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 * {1}{B}: Regenerate this creature.
 *
 * The black member of the Slith cycle (see [SlithFirewalker], [SlithPredator]). Same growth
 * trigger as the rest — [Triggers.DealsCombatDamageToPlayer], so *combat* damage only, and the
 * counter lands on the Slith itself ([EffectTarget.Self]) rather than a chosen creature.
 *
 * Black's rider is regeneration, which is what makes the compounding stick: the Bloodletter can
 * trade in combat and keep its accumulated counters, since a regeneration shield removes it from
 * combat and taps it without ever moving it to the graveyard (CR 701.15). Counters live on the
 * permanent, so they survive untouched.
 */
val SlithBloodletter = card("Slith Bloodletter") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Slith"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.\n" +
        "{1}{B}: Regenerate this creature."

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = RegenerateEffect(EffectTarget.Self)
        description = "{1}{B}: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Justin Sweet"
        flavorText = "Goblins fear the slith, believing they are children banished from the womb " +
            "of the Steel Mother, deep within Kuldotha."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22a04ccb-cb57-4548-81db-e1b77b09f5da.jpg?1783944545"
    }
}
