package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crosis, the Purger
 * {3}{U}{B}{R}
 * Legendary Creature — Dragon
 * 6/6
 * Flying
 * Whenever Crosis deals combat damage to a player, you may pay {2}{B}. If you do, choose a color,
 * then that player reveals their hand and discards all cards of that color.
 *
 * One of the five Coalition Dragons (cf. [RithTheAwakener], [TrevaTheRenewer]). The damaged
 * player is [Player.TriggeringPlayer]. Composes the chosen-color pattern with a reveal →
 * gather-by-chosen-color → discard pipeline (cf. [Addle], [Void]): [Effects.ChooseColorThen]
 * pauses for the color (chosen during resolution per the 2004-10-04 ruling), then every card of
 * that color in the triggering player's hand is discarded via [CardPredicate.HasChosenColor].
 */
val CrosisThePurger = card("Crosis, the Purger") {
    manaCost = "{3}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\nWhenever Crosis deals combat damage to a player, you may pay {2}{B}. If you do, " +
        "choose a color, then that player reveals their hand and discards all cards of that color."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}{B}"),
            effect = Effects.ChooseColorThen(
                then = Effects.Composite(
                    listOf(
                        RevealHandEffect(EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                zone = Zone.HAND,
                                player = Player.TriggeringPlayer,
                                filter = GameObjectFilter(
                                    cardPredicates = listOf(CardPredicate.HasChosenColor),
                                ),
                            ),
                            storeAs = "crosisDiscard",
                        ),
                        MoveCollectionEffect(
                            from = "crosisDiscard",
                            destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.TriggeringPlayer),
                            moveType = MoveType.Discard,
                        ),
                    ),
                ),
                prompt = "Choose a color",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5f336d8-12a4-482d-8ffd-c205858c72ba.jpg?1562941160"
    }
}
