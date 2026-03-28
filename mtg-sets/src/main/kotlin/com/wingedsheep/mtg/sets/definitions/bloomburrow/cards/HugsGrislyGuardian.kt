package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hugs, Grisly Guardian
 * {X}{R}{R}{G}{G}
 * Legendary Creature — Badger Warrior
 * 5/5
 *
 * Trample
 * When Hugs enters, exile the top X cards of your library. Until the end
 * of your next turn, you may play those cards.
 * You may play an additional land on each of your turns.
 *
 * Rulings:
 * - You pay all costs and follow all timing rules for cards played this way.
 * - You may play the exiled cards even if Hugs is no longer on the battlefield.
 * - The additional land effect is cumulative with similar effects.
 */
val HugsGrislyGuardian = card("Hugs, Grisly Guardian") {
    manaCost = "{X}{R}{R}{G}{G}"
    typeLine = "Legendary Creature — Badger Warrior"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "When Hugs enters, exile the top X cards of your library. Until the end of your next turn, you may play those cards.\n" +
        "You may play an additional land on each of your turns."

    keywords(Keyword.TRAMPLE)

    // When Hugs enters, exile the top X cards of your library.
    // Until the end of your next turn, you may play those cards.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.XValue),
                    storeAs = "exiledCards"
                ),
                MoveCollectionEffect(
                    from = "exiledCards",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                GrantMayPlayFromExileEffect(from = "exiledCards", untilEndOfNextTurn = true)
            )
        )
    }

    // You may play an additional land on each of your turns.
    // Modeled as a beginning-of-turn trigger that grants an extra land drop.
    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = PlayAdditionalLandsEffect(count = 1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Steve Prescott"
        flavorText = "Most of his scars tell tales. A few tell jokes."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f09d7f4a-c947-4389-befa-1d547d0d1237.jpg?1721427081"
        ruling("2024-07-26", "You pay all costs and follow all timing rules for cards played this way.")
        ruling("2024-07-26", "You may play the exiled cards even if Hugs is no longer on the battlefield or under your control.")
        ruling("2024-07-26", "The effect of Hugs's last ability that allows you to play an additional land is cumulative with similar effects.")
    }
}
