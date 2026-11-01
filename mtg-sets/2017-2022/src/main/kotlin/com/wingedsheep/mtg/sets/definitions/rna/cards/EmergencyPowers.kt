package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Emergency Powers
 * {5}{W}{U}
 * Instant
 * Each player shuffles their hand and graveyard into their library, then draws seven cards. Exile
 * Emergency Powers.
 * Addendum — If you cast this spell during your main phase, you may put a permanent card with mana
 * value 7 or less from your hand onto the battlefield.
 *
 * The wheel is Timetwister's two [Effects.ForEachPlayer] passes (everyone shuffles, then everyone
 * draws); the spell is still on the stack, so it never shuffles itself, and [selfExile] sends it to
 * exile as it finishes resolving. The addendum is checked at resolution like Precognitive Perception:
 * [Conditions.WasCast] excludes copies, [Conditions.IsYourMainPhase] reads the cast-time phase.
 * `putFromHand`'s up-to-one selection is the "may".
 */
val EmergencyPowers = card("Emergency Powers") {
    manaCost = "{5}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Each player shuffles their hand and graveyard into their library, then draws seven cards. " +
        "Exile Emergency Powers.\n" +
        "Addendum — If you cast this spell during your main phase, you may put a permanent card with mana " +
        "value 7 or less from your hand onto the battlefield."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            Effects.Pipeline {
                val handAndGraveyard = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                        player = Player.You,
                    )
                )
                move(
                    handAndGraveyard,
                    CardDestination.ToZone(
                        Zone.LIBRARY,
                        Player.You,
                        ZonePlacement.Shuffled,
                    )
                )
            },
        ) then Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7)) then
            Effects.If(
                condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
                then = Patterns.Hand.putFromHand(
                    filter = GameObjectFilter.Permanent.manaValueAtMost(7),
                    prompt = "You may put a permanent card with mana value 7 or less from your hand onto the battlefield",
                ),
            )
        selfExile()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "169"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6473a93f-879f-4f44-8650-ee05a647c763.jpg?1783933652"
        ruling(
            "2024-01-12",
            "Addendum abilities of instant spells apply while the spell is resolving, not immediately after " +
                "casting it. If the spell is countered, you don't get the addendum bonus.",
        )
        ruling(
            "2024-01-12",
            "If an effect copies a spell with an addendum ability while it's on the stack, the copy wasn't " +
                "cast at all, so you won't get the addendum bonus.",
        )
        ruling("2019-01-25", "If a card in your hand has {X} in its mana cost, X is considered to be 0.")
    }
}
