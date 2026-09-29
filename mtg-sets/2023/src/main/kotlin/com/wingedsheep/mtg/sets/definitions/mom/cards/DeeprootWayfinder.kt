package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deeproot Wayfinder (March of the Machine #184)
 * {1}{G} Creature — Merfolk Scout 2/3
 *
 * Whenever this creature deals combat damage to a player or battle, surveil 1, then you may
 * return a land card from your graveyard to the battlefield tapped.
 *
 * Surveil first, so a land just surveiled into the graveyard is a legal pick. "You may return a
 * land card" is untargeted and chosen on resolution: `chooseUpTo(1)` over the graveyard's land
 * cards (choosing none declines; an empty pool skips the prompt).
 */
val DeeprootWayfinder = card("Deeproot Wayfinder") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Scout"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature deals combat damage to a player or battle, surveil 1, then you " +
        "may return a land card from your graveyard to the battlefield tapped. (To surveil 1, look at " +
        "the top card of your library. You may put that card into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        effect = Effects.Pipeline {
            run(Effects.Surveil(1))
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
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Anna Pavleeva"
        flavorText = "Her warning raced through the roots and rivers of Ixalan: prepare for war."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4066d41-0eb5-4dfd-93ec-2f242c3a27a4.jpg?1783916970"
    }
}
