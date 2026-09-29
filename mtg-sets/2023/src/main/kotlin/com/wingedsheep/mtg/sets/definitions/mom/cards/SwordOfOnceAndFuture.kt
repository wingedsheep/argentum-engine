package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sword of Once and Future — March of the Machine #265
 * {3} · Artifact — Equipment · Mythic
 *
 * Equipped creature gets +2/+2 and has protection from blue and from black.
 * Whenever equipped creature deals combat damage to a player, surveil 2. Then you may cast an
 * instant or sorcery spell with mana value 2 or less from your graveyard without paying its mana
 * cost. If that spell would be put into your graveyard, exile it instead.
 * Equip {2}
 *
 * The free cast is not targeted: after the surveil, gather the instant/sorcery cards with mana
 * value 2 or less in your graveyard (including any just surveiled there), choose up to one, and
 * cast it during the trigger's resolution via [Effects.CastFromCollectionWithoutPayingCost] with
 * the [AfterResolveDestination.EXILE] rider — the Jetsam shape.
 */
val SwordOfOnceAndFuture = card("Sword of Once and Future") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and has protection from blue and from black.\n" +
        "Whenever equipped creature deals combat damage to a player, surveil 2. Then you may cast " +
        "an instant or sorcery spell with mana value 2 or less from your graveyard without paying " +
        "its mana cost. If that spell would be put into your graveyard, exile it instead.\n" +
        "Equip {2}"

    // Equipped creature gets +2/+2 ...
    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }
    // ... and has protection from blue and from black.
    staticAbility {
        ability = GrantProtection(Color.BLUE, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantProtection(Color.BLACK, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.surveil(2) then Effects.Pipeline {
            val candidates = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.InstantOrSorcery.manaValueAtMost(2),
                )
            )
            val toCast = chooseUpTo(
                1,
                from = candidates,
                prompt = "You may cast an instant or sorcery with mana value 2 or less from your graveyard without paying its mana cost",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(
                from = toCast,
                insteadOfGraveyard = AfterResolveDestination.EXILE,
            ))
        }
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "265"
        artist = "Joshua Cairos"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1670393d-86f0-46fb-b577-f73a1da2a3ed.jpg?1783916934"

        ruling(
            "2023-04-14",
            "You choose whether or not to cast the instant or sorcery card as the triggered ability " +
                "resolves. If you do, you do so as part of the resolution of that ability. You can't " +
                "wait to cast it later in the turn. Timing restrictions based on the card's type are ignored."
        )
        ruling(
            "2023-04-14",
            "The card you cast may be one you just put into the graveyard with surveil or one already " +
                "in the graveyard."
        )
        ruling(
            "2023-04-14",
            "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. " +
                "You can, however, pay additional costs. If the card has any mandatory additional costs, " +
                "those must be paid to cast the card."
        )
        ruling(
            "2023-04-14",
            "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it " +
                "without paying its mana cost."
        )
    }
}
