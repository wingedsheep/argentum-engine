package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heroism
 * {2}{W}
 * Enchantment
 * Sacrifice a white creature: For each attacking red creature, prevent all combat damage that would
 * be dealt by that creature this turn unless its controller pays {2}{R}.
 *
 * A per-creature ransom, and the payer is each attacking creature's *own* controller — which is why
 * the question is routed with [Player.ControllerOfIterationEntity] rather than to Heroism's
 * controller. Note the polarity: paying is what *avoids* the prevention.
 *
 * The tax is charged once per attacking red creature, so a red player attacking with four creatures
 * is asked four separate times and can buy back as many as they can afford.
 */
val Heroism = card("Heroism") {
    manaCost = "{2}{W}"
    colorIdentity = "WR"
    typeLine = "Enchantment"
    oracleText = "Sacrifice a white creature: For each attacking red creature, prevent all combat " +
        "damage that would be dealt by that creature this turn unless its controller pays {2}{R}."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature.withColor(Color.WHITE))
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.RED).attacking()),
            effect = PayOrSufferEffect(
                cost = Costs.pay.Mana("{2}{R}"),
                suffer = PreventDamageEffect(
                    target = EffectTarget.Self,
                    direction = PreventionDirection.FromTarget,
                    scope = PreventionScope.CombatOnly,
                    duration = Duration.EndOfTurn,
                ),
                player = EffectTarget.PlayerRef(Player.ControllerOfIterationEntity),
                consequenceDescription = "have all combat damage that creature would deal this turn prevented",
            )
        )
        description = "Sacrifice a white creature: For each attacking red creature, prevent all combat damage that would be dealt by that creature this turn unless its controller pays {2}{R}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08ee87a0-a7eb-4472-9045-85d11e8a1501.jpg?1783947919"
    }
}
