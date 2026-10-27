package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Teval, the Balanced Scale — Tarkir: Dragonstorm Commander #8 (canonical printing)
 * {1}{B}{G}{U} · Legendary Creature — Spirit Dragon · 4/4
 *
 * Flying
 * Whenever Teval attacks, mill three cards. Then you may return a land card from your graveyard
 * to the battlefield tapped.
 * Whenever one or more cards leave your graveyard, create a 2/2 black Zombie Druid creature token.
 *
 * Mill first, so a land just milled is a legal pick; the return is untargeted and chosen on
 * resolution (`chooseUpTo(1)` is the "may") — the Deeproot Wayfinder shape. "One or more cards
 * leave" is the CR 603.2c batch trigger, so a mass exile from the graveyard makes one token
 * (ruling); returning a land with the attack trigger is itself a card leaving the graveyard.
 */
val TevalTheBalancedScale = card("Teval, the Balanced Scale") {
    manaCost = "{1}{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever Teval attacks, mill three cards. Then you may return a land card from your graveyard " +
        "to the battlefield tapped.\n" +
        "Whenever one or more cards leave your graveyard, create a 2/2 black Zombie Druid creature token."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            mill(3)
            val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Land))
            val chosen = chooseUpTo(
                1,
                from = lands,
                showAllCards = true,
                prompt = "You may return a land card from your graveyard to the battlefield tapped",
                selectedLabel = "Return to battlefield",
                remainderLabel = "Leave in graveyard"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        }
        description = "Whenever Teval attacks, mill three cards. Then you may return a land card from your " +
            "graveyard to the battlefield tapped."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Druid"),
            imageUri = "https://cards.scryfall.io/normal/front/f/1/f10d5813-7818-43e8-b08d-4ed8c54d0366.jpg?1783906785"
        )
        description = "Whenever one or more cards leave your graveyard, create a 2/2 black Zombie Druid creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5d1955a-9ff0-4f8b-9af8-341371957299.jpg?1783907184"

        ruling("2025-04-04", "If multiple cards leave your graveyard at the same time, Teval's last ability will trigger only once.")
    }
}
