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
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dromar, the Banisher
 * {3}{W}{U}{B}
 * Legendary Creature — Dragon
 * 6/6
 * Flying
 * Whenever Dromar deals combat damage to a player, you may pay {2}{U}. If you do, choose a color,
 * then return all creatures of that color to their owners' hands.
 *
 * One of the five Coalition Dragons (cf. [RithTheAwakener], [TrevaTheRenewer]). After the optional
 * {2}{U} payment, [Effects.ChooseColorThen] pauses for the color, then every creature of that color
 * on the battlefield (any controller) is gathered via [CardPredicate.HasChosenColor] and returned to
 * its owner's hand — the creature-only variant of [WashOut]'s bounce pipeline.
 */
val DromarTheBanisher = card("Dromar, the Banisher") {
    manaCost = "{3}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\nWhenever Dromar deals combat damage to a player, you may pay {2}{U}. If you do, " +
        "choose a color, then return all creatures of that color to their owners' hands."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}{U}"),
            effect = Effects.ChooseColorThen(
                then = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.BattlefieldMatching(
                                filter = GameObjectFilter.Creature.withChosenColor(),
                                player = Player.Each,
                            ),
                            storeAs = "dromarBounce",
                        ),
                        MoveCollectionEffect(
                            from = "dromarBounce",
                            destination = CardDestination.ToZone(Zone.HAND),
                        ),
                    ),
                ),
                prompt = "Choose a color",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfcc3c72-fff5-454c-814c-eb952fd23ba9.jpg?1562936774"
    }
}
