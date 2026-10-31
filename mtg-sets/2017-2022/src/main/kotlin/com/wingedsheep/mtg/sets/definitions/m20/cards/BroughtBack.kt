package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brought Back
 * {W}{W}
 * Instant
 * Choose up to two target permanent cards in your graveyard that were put there from the
 * battlefield this turn. Return them to the battlefield tapped.
 *
 * Both cards return in one simultaneous move, so a returned Aura can't enchant the other
 * returning card and enters tapped like it.
 */
val BroughtBack = card("Brought Back") {
    manaCost = "{W}{W}"
    typeLine = "Instant"
    oracleText = "Choose up to two target permanent cards in your graveyard that were put there " +
        "from the battlefield this turn. Return them to the battlefield tapped."

    spell {
        targets(
            TargetFilter(
                GameObjectFilter.Permanent
                    .ownedByYou()
                    .putIntoGraveyardFromBattlefieldThisTurn(),
                zone = Zone.GRAVEYARD
            ),
            count = 2,
            optional = true
        )
        effect = Effects.Pipeline {
            val returning = gather(CardSource.ChosenTargets)
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Mitchell Malloy"
        flavorText = "\"As long as your courage remains, your life will not falter.\"\n—Sephara, Sky's Blade"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aecdc315-95e5-450c-bedc-998eea46cf8c.jpg?1783933031"
        ruling(
            "2025-10-02",
            "Permanent spells that were countered earlier in the turn never entered the battlefield, so they " +
                "won't be legal targets for Brought Back."
        )
        ruling(
            "2025-10-02",
            "If you return an Aura this way, you choose what the Aura will enchant just before it enters, but you " +
                "can't choose any permanent cards that will enter at the same time as that Aura. An Aura returning " +
                "to the battlefield this way doesn't target the player or permanent it will enchant, so permanents " +
                "or players with hexproof may be chosen; however, the chosen recipient must be able to legally be " +
                "enchanted by the Aura, so a player or permanent with protection from one of the Aura's qualities " +
                "can't be chosen this way. If there's nothing legal for the Aura to enchant, it stays in the graveyard."
        )
    }
}
