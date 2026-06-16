package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Caustic Bronco
 * {1}{B}
 * Creature — Snake Horse Mount
 * 2/2
 *
 * Whenever this creature attacks, reveal the top card of your library and put it into your hand.
 * You lose life equal to that card's mana value if this creature isn't saddled. Otherwise, each
 * opponent loses that much life.
 * Saddle 3 (Tap any number of other creatures you control with total power 3 or more: This Mount
 * becomes saddled until end of turn. Saddle only as a sorcery.)
 *
 * The attack trigger always fires (it is NOT gated on saddled). The reveal-to-hand happens
 * unconditionally; only the recipient of the life loss is decided by the saddled state at
 * resolution. The card's mana value is captured from the gathered collection via
 * [DynamicAmount.StoredCardManaValue], which survives the move to hand. "Saddled" is a state test
 * at resolution time, so a Mount unsaddled in response to the trigger correctly hits its own
 * controller.
 */
val CausticBronco = card("Caustic Bronco") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake Horse Mount"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, reveal the top card of your library and put it " +
        "into your hand. You lose life equal to that card's mana value if this creature isn't " +
        "saddled. Otherwise, each opponent loses that much life.\n" +
        "Saddle 3 (Tap any number of other creatures you control with total power 3 or more: This " +
        "Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywordAbility(KeywordAbility.saddle(3))

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.You),
                    storeAs = "revealed"
                ),
                MoveCollectionEffect(
                    from = "revealed",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You),
                    revealed = true
                ),
                ConditionalEffect(
                    condition = Conditions.SourceIsSaddled,
                    effect = LoseLifeEffect(
                        DynamicAmount.StoredCardManaValue("revealed"),
                        EffectTarget.PlayerRef(Player.EachOpponent)
                    ),
                    elseEffect = LoseLifeEffect(
                        DynamicAmount.StoredCardManaValue("revealed"),
                        EffectTarget.Controller
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9a268ba-c442-4fe4-90b4-2810c8474f4e.jpg?1712355566"
    }
}
