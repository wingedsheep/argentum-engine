package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sunspine Lynx
 * {2}{R}{R}
 * Creature — Elemental Cat
 * 5/4
 * Players can't gain life.
 * Damage can't be prevented.
 * When Sunspine Lynx enters, it deals damage to each player equal to the number of
 * nonbasic lands that player controls.
 */
val SunspineLynx = card("Sunspine Lynx") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Elemental Cat"
    power = 5
    toughness = 4
    oracleText = "Players can't gain life.\nDamage can't be prevented.\nWhen Sunspine Lynx enters, it deals damage to each player equal to the number of nonbasic lands that player controls."

    replacementEffect(PreventLifeGain(appliesTo = GameEvent.LifeGainEvent(player = Player.Each)))
    replacementEffect(DamageCantBePrevented())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                Effects.DealDamage(
                    amount = DynamicAmount.Count(
                        player = Player.You,
                        zone = Zone.BATTLEFIELD,
                        filter = GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.IsLand,
                                CardPredicate.Not(CardPredicate.IsBasicLand)
                            )
                        )
                    ),
                    target = EffectTarget.Controller
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Martin Wittfooth"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8995ceaf-b7e0-423c-8f3e-25212d522502.jpg?1721426719"
        ruling("2024-07-26", "Spells and abilities that cause players to gain life still resolve while Sunspine Lynx is on the battlefield. No player will gain life, but any other effects of that spell or ability will happen.")
        ruling("2024-07-26", "If an effect says to set a player's life total to a number that's higher than the player's current life total while Sunspine Lynx is on the battlefield, the player's life total doesn't change.")
        ruling("2024-07-26", "Sunspine Lynx only stops damage from being prevented by effects that specifically use the word 'prevent.'")
        ruling("2024-07-26", "Protection prevents damage, so protection will be unable to prevent damage while Sunspine Lynx is on the battlefield.")
    }
}
