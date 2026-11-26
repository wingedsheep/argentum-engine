package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shimian Specter
 * {2}{B}{B}
 * Creature — Specter
 * 2/2
 * Flying
 * Whenever this creature deals combat damage to a player, that player reveals their hand.
 * You choose a nonland card from it. Search that player's graveyard, hand, and library for
 * all cards with the same name as that card and exile them. Then that player shuffles.
 *
 * The [Lobotomy] pipeline driven off a combat-damage trigger: the damaged player is
 * [Player.TriggeringPlayer]. With no nonland card in hand nothing is chosen, so the search
 * finds nothing — but the library is still searched and shuffled (2012-07-01 ruling).
 */
val ShimianSpecter = card("Shimian Specter") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Specter"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever this creature deals combat damage to a player, that player reveals their hand. " +
        "You choose a nonland card from it. Search that player's graveyard, hand, and library for all cards with " +
        "the same name as that card and exile them. Then that player shuffles."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val damaged = Player.TriggeringPlayer
            run(Effects.RevealHand(EffectTarget.PlayerRef(damaged)))
            val hand = gather(CardSource.FromZone(Zone.HAND, damaged))
            val chosen = chooseExactly(
                1, from = hand,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card",
                alwaysPrompt = true,
                showAllCards = true
            )
            val chosenName = storeCardName(chosen)
            val toExile = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = damaged,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                ),
                search = true
            )
            exile(toExile, owner = damaged)
            run(Effects.ShuffleLibrary(target = EffectTarget.PlayerRef(damaged)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6faa406-aa7a-49ce-a42e-00e98f3fb74e.jpg?1783943111"
        ruling(
            "2012-07-01",
            "The cards you're searching for must be found and exiled if they're in the graveyard because it's a " +
                "public zone. Finding those cards in the hand and library is optional, because those zones are " +
                "hidden (even if the hand is temporarily revealed)."
        )
        ruling(
            "2012-07-01",
            "If the player has no nonland cards in their hand, you can still search that player's library and " +
                "have them shuffle it."
        )
    }
}
