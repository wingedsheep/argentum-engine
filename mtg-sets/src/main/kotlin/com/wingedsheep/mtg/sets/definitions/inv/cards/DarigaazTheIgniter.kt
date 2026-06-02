package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Darigaaz, the Igniter
 * {3}{B}{R}{G}
 * Legendary Creature — Dragon
 * 6/6
 * Flying
 * Whenever Darigaaz deals combat damage to a player, you may pay {2}{R}. If you do, choose a color,
 * then that player reveals their hand and Darigaaz deals damage to the player equal to the number
 * of cards of that color revealed this way.
 *
 * One of the five Coalition Dragons (cf. [RithTheAwakener], [TrevaTheRenewer]). The damaged player
 * is [Player.TriggeringPlayer]. After the optional {2}{R} payment, [Effects.ChooseColorThen]
 * pauses for the color, the player reveals their hand, then Darigaaz deals damage equal to the
 * count of chosen-color cards in that revealed hand — modeled as an `AggregateZone` over the
 * triggering player's hand filtered by [CardPredicate.HasChosenColor].
 */
val DarigaazTheIgniter = card("Darigaaz, the Igniter") {
    manaCost = "{3}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\nWhenever Darigaaz deals combat damage to a player, you may pay {2}{R}. If you do, " +
        "choose a color, then that player reveals their hand and Darigaaz deals damage to the player " +
        "equal to the number of cards of that color revealed this way."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}{R}"),
            effect = Effects.ChooseColorThen(
                then = Effects.Composite(
                    listOf(
                        RevealHandEffect(EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                        Effects.DealDamage(
                            amount = DynamicAmounts.zone(
                                player = Player.TriggeringPlayer,
                                zone = Zone.HAND,
                                filter = GameObjectFilter(
                                    cardPredicates = listOf(CardPredicate.HasChosenColor),
                                ),
                            ).count(),
                            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                            damageSource = EffectTarget.Self,
                        ),
                    ),
                ),
                prompt = "Choose a color",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54dcf5e3-4303-41a3-b54c-24a9d462ce07.jpg?1562912267"
    }
}
