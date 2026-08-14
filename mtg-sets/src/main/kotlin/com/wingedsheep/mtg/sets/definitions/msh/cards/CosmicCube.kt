package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cosmic Cube — Marvel Super Heroes #245 (mythic)
 * {5} · Artifact
 *
 * Ward {2}
 * Whenever you attack, look at the top six cards of your library. You may cast a spell from among
 * them with mana value less than or equal to the greatest power among attacking creatures you
 * control without paying its mana cost. Put the rest on the bottom of your library in a random
 * order.
 *
 * The Sunbird's Invocation pipeline with two substitutions, so the mechanic stays four atomic
 * pipeline steps and the cards never leave the library:
 *
 *  1. [GatherCardsEffect] over the top six — `revealed` stays false, because this is "look at",
 *     not "reveal": only the controller sees the pile.
 *  2. [SelectFromCollectionEffect] `ChooseUpTo(1)`, eligibility = nonland (a *spell*) whose mana
 *     value is at most the greatest power among your attacking creatures — an
 *     [DynamicAmount.AggregateBattlefield] MAX over [CardNumericProperty.POWER] filtered to
 *     `Creature.attacking().youControl()`, evaluated at resolution off projected power, so a pump
 *     that landed after attackers were declared counts. `showAllCards = true` still shows the
 *     ineligible cards (greyed out), and the player may confirm without choosing anything.
 *  3. The remainder goes to the *bottom* of the library in a random order ([CardOrder.Random]),
 *     replacing Sunbird's identical bottoming step.
 *  4. [CastFromCollectionWithoutPayingCostEffect] casts the chosen card straight out of the
 *     library via a single-card play permission; if the cast pauses for targets, X, or modes, the
 *     pipeline pauses with it.
 *
 * "Whenever you attack" ([Triggers.YouAttack]) triggers once per combat in which you attack with
 * one or more creatures — not once per attacker — and only on your turn, since only the attacking
 * player declares attackers.
 */
val CosmicCube = card("Cosmic Cube") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Ward {2}\n" +
        "Whenever you attack, look at the top six cards of your library. You may cast a spell " +
        "from among them with mana value less than or equal to the greatest power among attacking " +
        "creatures you control without paying its mana cost. Put the rest on the bottom of your " +
        "library in a random order."

    keywordAbility(KeywordAbility.ward("{2}"))

    triggeredAbility {
        trigger = Triggers.YouAttack
        val greatestAttackingPower = DynamicAmount.AggregateBattlefield(
            player = Player.You,
            filter = GameObjectFilter.Creature.attacking(),
            aggregation = Aggregation.MAX,
            property = CardNumericProperty.POWER
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.Fixed(6),
                        player = Player.You
                    ),
                    storeAs = "cosmicCubeLooked"
                ),
                SelectFromCollectionEffect(
                    from = "cosmicCubeLooked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland
                        .manaValueAtMostDynamic(greatestAttackingPower),
                    storeSelected = "cosmicCubeChosen",
                    storeRemainder = "cosmicCubeToBottom",
                    showAllCards = true,
                    prompt = "You may cast a spell from among these cards without paying its " +
                        "mana cost.",
                    selectedLabel = "Cast for free",
                    remainderLabel = "Put on the bottom"
                ),
                MoveCollectionEffect(
                    from = "cosmicCubeToBottom",
                    destination = CardDestination.ToZone(
                        Zone.LIBRARY,
                        placement = ZonePlacement.Bottom
                    ),
                    order = CardOrder.Random
                ),
                CastFromCollectionWithoutPayingCostEffect(from = "cosmicCubeChosen")
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1cf1ead-fe91-4328-89ab-6d0bc9ff6cbe.jpg?1783902891"
    }
}
