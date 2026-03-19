package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.references.Player

/**
 * See the Unwritten
 * {4}{G}{G}
 * Sorcery
 * Reveal the top eight cards of your library. You may put a creature card from among them
 * onto the battlefield. Put the rest into your graveyard.
 * Ferocious — If you control a creature with power 4 or greater, you may put two creature
 * cards onto the battlefield instead of one.
 */
val SeeTheUnwritten = card("See the Unwritten") {
    manaCost = "{4}{G}{G}"
    typeLine = "Sorcery"
    oracleText = "Reveal the top eight cards of your library. You may put a creature card from among them onto the battlefield. Put the rest into your graveyard.\nFerocious — If you control a creature with power 4 or greater, you may put two creature cards onto the battlefield instead of one."

    spell {
        val ferocious = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4))

        fun selectAndMove(count: Int) = CompositeEffect(
            listOf(
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                    filter = GameObjectFilter.Creature,
                    storeSelected = "selected",
                    storeRemainder = "rest",
                    prompt = "Choose creature card${if (count > 1) "s" else ""} to put onto the battlefield",
                    selectedLabel = "Put onto the battlefield",
                    remainderLabel = "Put into your graveyard",
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "selected",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                )
            )
        )

        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(8)),
                    storeAs = "revealed",
                    revealed = true
                ),
                ConditionalEffect(
                    condition = ferocious,
                    effect = selectAndMove(2),
                    elseEffect = selectAndMove(1)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3689a7f1-9713-412e-a030-9cba463c170e.jpg?1562784794"
        ruling("2014-09-20", "If a card you put onto the battlefield this way has {X} in its mana cost, X is considered to be 0.")
        ruling("2014-09-20", "You determine whether the ferocious ability applies before any creature cards are put onto the battlefield.")
        ruling("2014-09-20", "Because you're not casting the creature cards, you can't pay any additional or alternative costs.")
    }
}
