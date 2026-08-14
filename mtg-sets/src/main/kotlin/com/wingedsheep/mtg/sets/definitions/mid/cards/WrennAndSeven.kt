package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wrenn and Seven — Innistrad: Midnight Hunt #208
 * {3}{G}{G} · Legendary Planeswalker — Wrenn · Starting loyalty 5
 *
 * +1: Reveal the top four cards of your library. Put all land cards revealed this way into your
 *     hand and the rest into your graveyard.
 * 0: Put any number of land cards from your hand onto the battlefield tapped.
 * −3: Create a green Treefolk creature token with reach and "This token's power and toughness are
 *     each equal to the number of lands you control."
 * −8: Return all permanent cards from your graveyard to your hand. You get an emblem with "You have
 *     no maximum hand size."
 *
 * Modeling notes:
 *
 *  - **The +1 splits one revealed pile by type**, the Sméagol, Helpful Guide idiom: a single
 *    [GatherCardsEffect] with `revealed = true`, then two [MoveCollectionEffect]s over the *same*
 *    collection with complementary `filter`s (Land → hand, Nonland → graveyard). Gathering twice
 *    would reveal eight cards; filtering the move is what makes "revealed this way" mean one pile.
 *  - **The 0 is a free choice, not a land drop** — it bypasses the one-land-per-turn rule because
 *    it *puts* lands onto the battlefield rather than playing them, so it is a
 *    Gather → Select → Move pipeline over the hand and never touches the land-drop counter.
 *    [SelectionMode.ChooseAnyNumber] honors "any number", including zero, and
 *    [ZonePlacement.Tapped] is the printed "tapped".
 *  - **The −3 token's P/T is a characteristic-defining ability, not a snapshot.**
 *    `Effects.CreateDynamicToken` would be wrong here: `CreateTokenExecutor` evaluates
 *    `dynamicPower` *once, at creation*, freezing the token at however many lands you had that
 *    turn. The printed ability recalculates continuously, so the P/T lives on the token as a
 *    [SetBasePowerToughnessDynamicStatic] over [GroupFilter.source] — a Layer 7b continuous effect
 *    re-evaluated on every projection (Thousand Moons Smithy's idiom). Play a land, the Treefolk
 *    grows; lose one, it shrinks.
 *  - **"You control" follows the token.** The count is `Player.You` evaluated against the *token*
 *    as the source, so if an opponent gains control of the Treefolk its P/T starts counting their
 *    lands — which is what the printed CDA says.
 *  - **The −8 emblem is [Effects.RemoveMaximumHandSize]**, the player-scoped permanent property
 *    (Finale of Revelation, Wisdom of Ages). `CreatePermanentEmblemEffect` only carries P/T and
 *    keyword grants over a group filter, so it cannot express a player characteristic; this
 *    primitive confers exactly the emblem's effect and, like an emblem, can never be removed.
 *  - **"All permanent cards"** is `GameObjectFilter.Permanent` read out of the graveyard — the
 *    Rydia's Return idiom — so lands and planeswalkers come back too, and instants/sorceries stay.
 */

/** The number of lands the Treefolk token's controller controls — re-read on every projection. */
private val landsYouControl: DynamicAmount = DynamicAmounts.landsYouControl()

val WrennAndSeven = card("Wrenn and Seven") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Wrenn"
    startingLoyalty = 5
    oracleText = "+1: Reveal the top four cards of your library. Put all land cards revealed this " +
        "way into your hand and the rest into your graveyard.\n" +
        "0: Put any number of land cards from your hand onto the battlefield tapped.\n" +
        "−3: Create a green Treefolk creature token with reach and \"This token's power and " +
        "toughness are each equal to the number of lands you control.\"\n" +
        "−8: Return all permanent cards from your graveyard to your hand. You get an emblem with " +
        "\"You have no maximum hand size.\""

    // +1: Reveal the top four cards of your library. Put all land cards revealed this way into
    //     your hand and the rest into your graveyard.
    loyaltyAbility(+1) {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                    storeAs = "revealed",
                    revealed = true
                ),
                MoveCollectionEffect(
                    from = "revealed",
                    filter = GameObjectFilter.Land,
                    destination = CardDestination.ToZone(Zone.HAND, Player.You)
                ),
                MoveCollectionEffect(
                    from = "revealed",
                    filter = GameObjectFilter.Nonland,
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You)
                )
            ),
            descriptionOverride = "Reveal the top four cards of your library. Put all land cards " +
                "revealed this way into your hand and the rest into your graveyard."
        )
    }

    // 0: Put any number of land cards from your hand onto the battlefield tapped.
    loyaltyAbility(0) {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.HAND,
                        player = Player.You,
                        filter = GameObjectFilter.Land
                    ),
                    storeAs = "lands_in_hand"
                ),
                SelectFromCollectionEffect(
                    from = "lands_in_hand",
                    selection = SelectionMode.ChooseAnyNumber,
                    chooser = Chooser.Controller,
                    storeSelected = "chosen_lands",
                    prompt = "Choose any number of land cards to put onto the battlefield tapped."
                ),
                MoveCollectionEffect(
                    from = "chosen_lands",
                    destination = CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        Player.You,
                        ZonePlacement.Tapped
                    )
                )
            ),
            descriptionOverride = "Put any number of land cards from your hand onto the " +
                "battlefield tapped."
        )
    }

    // −3: Create a green Treefolk creature token with reach and "This token's power and toughness
    //     are each equal to the number of lands you control."
    loyaltyAbility(-3) {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Treefolk"),
            keywords = setOf(Keyword.REACH),
            imageUri = "https://cards.scryfall.io/normal/front/9/4/94e4345b-61b1-4026-a01c-c9f2036c5c8a.jpg?1783925221",
            staticAbilities = listOf(
                SetBasePowerToughnessDynamicStatic(
                    power = landsYouControl,
                    toughness = landsYouControl,
                    filter = GroupFilter.source()
                )
            )
        )
    }

    // −8: Return all permanent cards from your graveyard to your hand. You get an emblem with
    //     "You have no maximum hand size."
    loyaltyAbility(-8) {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.Permanent
                    ),
                    storeAs = "permanents_in_graveyard"
                ),
                MoveCollectionEffect(
                    from = "permanents_in_graveyard",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You)
                ),
                Effects.RemoveMaximumHandSize()
            ),
            descriptionOverride = "Return all permanent cards from your graveyard to your hand. " +
                "You get an emblem with \"You have no maximum hand size.\""
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "208"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7757e99-8d51-4b92-b346-6961845def24.jpg?1783925567"
    }
}
