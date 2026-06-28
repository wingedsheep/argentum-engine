package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Clive's Hideaway
 * Land — Town
 *
 * Hideaway 4 (When this land enters, look at the top four cards of your library, exile one
 * face down, then put the rest on the bottom in a random order.)
 * {T}: Add {C}.
 * {2}, {T}: You may play the exiled card without paying its mana cost if you control four or
 * more legendary creatures.
 *
 * Modeled on the classic Hideaway 4 land (see Mosswort Bridge): the ETB trigger gathers the
 * top four cards, prompts the controller to exile one face down and linked to this land, and
 * bottom-randomizes the rest. The activated free-cast ability gathers from the linked exile
 * and grants may-play + without-paying-cost. The "four or more legendary creatures" clause is
 * modeled as an activation restriction so the ability cannot be activated otherwise. Modern
 * Hideaway no longer enters tapped (CR errata 2022-04-29), and Clive's Hideaway has no
 * "enters tapped" line, so no [EntersTapped] replacement is added.
 */
val ClivesHideaway = card("Clive's Hideaway") {
    typeLine = "Land — Town"
    oracleText = "Hideaway 4 (When this land enters, look at the top four cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "{T}: Add {C}.\n" +
        "{2}, {T}: You may play the exiled card without paying its mana cost if you control " +
        "four or more legendary creatures."

    keywordAbility(KeywordAbility.hideaway(4))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.Fixed(4),
                        player = Player.You
                    ),
                    storeAs = "hideawayTop"
                ),
                SelectFromCollectionEffect(
                    from = "hideawayTop",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "hideawayPicked",
                    storeRemainder = "hideawayRest",
                    prompt = "Choose a card to exile face down",
                    selectedLabel = "Exile face down",
                    remainderLabel = "Put on bottom of library"
                ),
                MoveCollectionEffect(
                    from = "hideawayPicked",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    faceDown = FaceDownMode.HIDDEN,
                    linkToSource = true
                ),
                MoveCollectionEffect(
                    from = "hideawayRest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    order = CardOrder.Random
                )
            )
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromLinkedExile(),
                    storeAs = "hideawayLinked"
                ),
                GrantMayPlayFromExileEffect("hideawayLinked"),
                GrantPlayWithoutPayingCostEffect("hideawayLinked")
            )
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Compare(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.legendary()).count(),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(4)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "275"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e43c36f-b8a2-4b2b-b2ea-57e6fa97521c.jpg?1748706815"
    }
}
