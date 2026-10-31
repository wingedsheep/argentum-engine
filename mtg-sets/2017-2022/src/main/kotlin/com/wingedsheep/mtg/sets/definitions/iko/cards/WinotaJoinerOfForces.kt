package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Winota, Joiner of Forces
 * {2}{R}{W}
 * Legendary Creature — Human Warrior
 * 4/4
 *
 * Whenever a non-Human creature you control attacks, look at the top six cards of your library.
 * You may put a Human creature card from among them onto the battlefield tapped and attacking.
 * It gains indestructible until end of turn. Put the rest of the cards on the bottom of your
 * library in a random order.
 *
 * - The trigger fires once per non-Human attacker (`Triggers.a(...).attacks()`, Fervent Charge's
 *   shape); Winota herself is a Human, so the filter never matches her.
 * - Look/choose/rest is Arthur, Marigold Knight's pipeline; the kept card is `moveTracked` so the
 *   indestructible grant lands only on the Human that actually entered (none if declined).
 */
val WinotaJoinerOfForces = card("Winota, Joiner of Forces") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 4
    oracleText = "Whenever a non-Human creature you control attacks, look at the top six cards of your " +
        "library. You may put a Human creature card from among them onto the battlefield tapped and " +
        "attacking. It gains indestructible until end of turn. Put the rest of the cards on the bottom " +
        "of your library in a random order."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().notSubtype(Subtype.HUMAN)).attacks()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
                prompt = "You may put a Human creature card onto the battlefield tapped and attacking",
                selectedLabel = "Put onto the battlefield tapped and attacking",
                remainderLabel = "Put on the bottom of your library",
                showAllCards = true
            )
            val entered = moveTracked(
                kept,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.TappedAndAttacking)
            ).asTarget
            run(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, entered, Duration.EndOfTurn))
            move(rest, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom), order = CardOrder.Random)
        }
        description = "Whenever a non-Human creature you control attacks, look at the top six cards of " +
            "your library. You may put a Human creature card from among them onto the battlefield " +
            "tapped and attacking. It gains indestructible until end of turn. Put the rest of the " +
            "cards on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "216"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dd13a6c-23d3-44ce-a628-cb1c19d777c4.jpg?1783931014"

        ruling("2020-04-17", "If more than one non-Human creature you control attacks, Winota's ability triggers separately for each of them. For example, if three non-Human creatures attack, you won't look at the top eighteen cards of your library at once; you'll look at six, finish resolving the ability, and then repeat the process twice more as the other instances of the ability resolve.")
        ruling("2020-04-17", "You choose which player or planeswalker the Human is attacking. It doesn't have to be attacking the same player or planeswalker that the non-Human creature is attacking.")
        ruling("2020-04-17", "Although the Human is an attacking creature, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks won't trigger when it enters the battlefield attacking.")
        ruling("2020-04-17", "Any effects that say that the Human can't attack (such as that of Propaganda, or if you get a Human with defender) affect only the declaration of attackers. They won't stop the Human you find from entering the battlefield attacking.")
    }
}
