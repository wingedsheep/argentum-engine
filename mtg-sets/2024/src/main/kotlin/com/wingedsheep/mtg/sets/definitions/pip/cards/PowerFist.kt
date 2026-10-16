package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Power Fist (PIP #81) — {1}{G} Artifact — Equipment
 *
 * Equipped creature has trample and "Whenever this creature deals combat damage to a player,
 * put that many +1/+1 counters on it."
 * Equip {2}
 *
 * The trigger is granted to the equipped creature, so "it" is that creature — not the Equipment.
 * [EffectTarget.Self] inside a [GrantTriggeredAbility] resolves to the bearer, and "that many" is
 * the damage just dealt to the player ([DynamicAmounts.triggerDamageAmount]).
 */
val PowerFist = card("Power Fist") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has trample and \"Whenever this creature deals combat damage to a player, " +
        "put that many +1/+1 counters on it.\"\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.AddDynamicCounters(
                    CounterType.PLUS_ONE_PLUS_ONE,
                    DynamicAmounts.triggerDamageAmount(),
                    EffectTarget.Self
                )
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Campbell White"
        flavorText = "A fist fight will leave a bruise. A power fist fight will leave a crater."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e8f7fbc-bf2a-43c1-a88d-807c1e7dab69.jpg?1783912396"
    }
}
