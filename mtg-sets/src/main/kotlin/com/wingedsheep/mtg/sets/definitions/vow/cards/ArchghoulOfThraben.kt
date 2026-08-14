package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Archghoul of Thraben
 * {2}{B}
 * Creature — Zombie Cleric
 * 3/2
 *
 * Whenever this creature or another Zombie you control dies, look at the top card of your library.
 * If it's a Zombie card, you may reveal it and put it into your hand. If you don't put the card into
 * your hand, you may put it into your graveyard.
 *
 * "This creature **or another** Zombie you control" is [TriggerBinding.ANY] over a Zombie-you-control
 * death — the source counts itself, and per the 2021-11-19 ruling a simultaneous death of Archghoul
 * plus another Zombie triggers once per dying Zombie (the per-event `ZoneChangeEvent`, not a batch).
 *
 * The resolution is a plain Gather → Select → Move pipeline over the top card, in two stages so both
 * "may"s are honored independently:
 *  1. gather the top card (a *look*, not a move), then offer up to one **Zombie** card to reveal into
 *     hand — declining keeps it in the remainder;
 *  2. whatever wasn't taken is offered again, unfiltered, for the optional trip to the graveyard;
 *     anything still left goes back on top, undisturbed.
 *
 * Neither select sets `showAllCards`, so a non-Zombie top card skips stage 1's prompt entirely rather
 * than opening a modal with nothing selectable: the player then sees exactly one modal — stage 2's,
 * which shows them the card and offers the only real choice. A Zombie top card gets two modals only
 * because it genuinely carries two decisions.
 *
 * Deliberately not `Patterns.Library.surveil(1)` for stage 2 — surveil emits a `SurveiledEvent` and
 * would fire unrelated surveil payoffs on a card that never surveils.
 */
val ArchghoulOfThraben = card("Archghoul of Thraben") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Cleric"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature or another Zombie you control dies, look at the top card of " +
        "your library. If it's a Zombie card, you may reveal it and put it into your hand. If you " +
        "don't put the card into your hand, you may put it into your graveyard."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "looked"
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE),
                storeSelected = "toHand",
                storeRemainder = "notTaken",
                prompt = "Reveal the Zombie card and put it into your hand?",
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave it"
            ),
            MoveCollectionEffect(
                from = "toHand",
                destination = CardDestination.ToZone(Zone.HAND),
                revealed = true
            ),
            SelectFromCollectionEffect(
                from = "notTaken",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "toGraveyard",
                storeRemainder = "staysOnTop",
                prompt = "Put the card into your graveyard?",
                selectedLabel = "Put into your graveyard",
                remainderLabel = "Leave on top of your library"
            ),
            MoveCollectionEffect(
                from = "toGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            ),
            MoveCollectionEffect(
                from = "staysOnTop",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
            )
        )
        description = "Whenever this creature or another Zombie you control dies, look at the top " +
            "card of your library. If it's a Zombie card, you may reveal it and put it into your " +
            "hand. If you don't put the card into your hand, you may put it into your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cf81c9d-ddb2-470e-8a4a-590049713e95.jpg?1783924876"

        ruling(
            "2021-11-19",
            "If Archghoul of Thraben and another Zombie you control die at the same time, this " +
                "ability will trigger once for each of them."
        )
    }
}
