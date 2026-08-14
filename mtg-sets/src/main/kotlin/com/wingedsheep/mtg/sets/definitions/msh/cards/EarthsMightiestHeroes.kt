package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Earth's Mightiest Heroes — Marvel Super Heroes #165
 * {4}{G}{G} · Sorcery
 *
 * Teamwork 5 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 5 or more.)
 * Reveal the top eight cards of your library. You may put a creature card from among them onto the
 * battlefield. If this spell was cast using teamwork, put any number of creature cards from among
 * them onto the battlefield instead. Put the rest into your graveyard.
 *
 * The plain spell-rider "instead" shape of teamwork (CR 702.194b) over the Gather → Select → Move
 * pipeline. Only the *selection mode* changes, so the reveal and both moves are shared and the
 * [ConditionalEffect] wraps just the select-and-move half — structurally the same split See the
 * Unwritten uses for its ferocious "two instead of one".
 *
 * The two modes are the printed wordings, not an approximation of them:
 * [SelectionMode.ChooseAnyNumber] is "any number of creature cards" and
 * [SelectionMode.ChooseUpTo]`(1)` is "you may put *a* creature card" — the "may" is the "up to".
 * Both are still gated by the creature filter, so noncreature cards are never selectable and land
 * in the graveyard with the rest.
 *
 * [Conditions.TeamworkWasPaid] is read while the spell is still on the stack, at the moment the
 * conditional is evaluated during resolution — the same reading Helicarrier Strike relies on.
 */
val EarthsMightiestHeroes = card("Earth's Mightiest Heroes") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Teamwork 5 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 5 or more.)\n" +
        "Reveal the top eight cards of your library. You may put a creature card from among them " +
        "onto the battlefield. If this spell was cast using teamwork, put any number of creature " +
        "cards from among them onto the battlefield instead. Put the rest into your graveyard."

    teamwork(5)

    spell {
        fun selectAndMove(selection: SelectionMode, prompt: String) = Effects.Composite(
            SelectFromCollectionEffect(
                from = "revealed",
                selection = selection,
                filter = GameObjectFilter.Creature,
                storeSelected = "selected",
                storeRemainder = "rest",
                prompt = prompt,
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put into your graveyard",
                showAllCards = true,
            ),
            MoveCollectionEffect(
                from = "selected",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD),
            ),
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
            ),
        )

        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(8)),
                storeAs = "revealed",
                revealed = true,
            ),
            ConditionalEffect(
                condition = Conditions.TeamworkWasPaid,
                effect = selectAndMove(
                    SelectionMode.ChooseAnyNumber,
                    "Choose any number of creature cards to put onto the battlefield",
                ),
                elseEffect = selectAndMove(
                    SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    "Choose a creature card to put onto the battlefield",
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "165"
        artist = "Steve Morris"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b38b9bd6-1dd7-4bbc-8a82-ce391c1172e1.jpg?1783902919"
    }
}
