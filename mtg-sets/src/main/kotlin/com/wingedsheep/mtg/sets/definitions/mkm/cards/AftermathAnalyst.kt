package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aftermath Analyst — Murders at Karlov Manor #148
 * {1}{G} · Creature — Elf Detective · 1/3
 *
 * When this creature enters, mill three cards.
 * {3}{G}, Sacrifice this creature: Return all land cards from your graveyard to the battlefield
 * tapped.
 *
 * The two halves are one engine: the enters trigger stocks the graveyard with lands, and the
 * activated ability cashes them all in. Because the mill is a *may-less* trigger it happens even
 * when it hits no lands — the ramp payoff simply reads whatever is in the graveyard at the moment
 * the ability resolves, including lands that got there long before this Elf arrived.
 *
 * The return half is the Lumra, Bellow of the Woods shape: gather every land card in the
 * controller's graveyard into a named collection, then move that whole collection to the
 * battlefield tapped. Gathering first is what makes "all land cards" a single snapshot — cards put
 * into the graveyard while the ability is on the stack are included (the gather happens on
 * resolution), but nothing is re-scanned mid-move.
 *
 * `Costs.SacrificeSelf` pays before resolution (CR 601.2h), so Aftermath Analyst is already in the
 * graveyard when the ability resolves — it is a creature card, not a land, so it never returns
 * itself.
 */
val AftermathAnalyst = card("Aftermath Analyst") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Detective"
    oracleText = "When this creature enters, mill three cards. (Put the top three cards of your " +
        "library into your graveyard.)\n" +
        "{3}{G}, Sacrifice this creature: Return all land cards from your graveyard to the " +
        "battlefield tapped."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.mill(3)
        description = "When this creature enters, mill three cards."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}"), Costs.SacrificeSelf)
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land),
                storeAs = "graveyard_lands",
            ),
            MoveCollectionEffect(
                from = "graveyard_lands",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
            ),
        )
        description = "Return all land cards from your graveyard to the battlefield tapped"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Danny Schwartz"
        flavorText = "\"Could someone bring me more evidence markers? Like . . . a lot more?\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c1aa6f8-2d34-4f4b-9184-0eab2e4745f7.jpg?1783912870"
    }
}
