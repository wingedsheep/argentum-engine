package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Mosswort Bridge
 * Land
 *
 * Hideaway 4 (When this land enters, look at the top four cards of your library, exile
 * one face down, then put the rest on the bottom in a random order.)
 * This land enters tapped.
 * {T}: Add {G}.
 * {G}, {T}: You may play the exiled card without paying its mana cost if creatures you
 * control have total power 10 or greater.
 *
 * Hideaway is modeled compositionally (see [com.wingedsheep.mtg.sets.definitions.blc.cards.EvercoatUrsine]):
 * the ETB trigger gathers the top four cards of the controller's library, prompts them
 * to exile one face down and linked to this land, then bottom-randomizes the rest. The
 * activated free-cast ability gathers from the linked exile and grants may-play +
 * without-paying-cost. The power-10 clause is modeled as an activation restriction so
 * the ability cannot be activated unless creatures the controller controls total 10 or
 * more power.
 */
val MosswortBridge = card("Mosswort Bridge") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "Hideaway 4 (When this land enters, look at the top four cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "This land enters tapped.\n" +
        "{T}: Add {G}.\n" +
        "{G}, {T}: You may play the exiled card without paying its mana cost if creatures " +
        "you control have total power 10 or greater."

    keywordAbility(KeywordAbility.hideaway(4))

    replacementEffect(EntersTapped())

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
                    faceDown = true,
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
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
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
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(10)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "270"
        artist = "Jeremy Jarvis"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38234590-812c-4d29-80c1-32b9e1282580.jpg?1562344458"
        ruling("2022-04-29", "\"Hideaway N\" means \"When this permanent enters the battlefield, look at the top N cards of your library. Exile one of them face down and put the rest on the bottom of your library in a random order. The exiled card gains 'The player who controls the permanent that exiled this card may look at this card in the exile zone.'\"")
        ruling("2022-04-29", "Any player who has controlled a permanent with a hideaway ability since a card was exiled with it may look at that card.")
        ruling("2022-04-29", "Previously, permanents with hideaway entered the battlefield tapped. This ability has been removed from the definition of hideaway. Older cards have received errata to have an additional paragraph that reads \"[This permanent] enters the battlefield tapped,\" and they now have hideaway 4.")
        ruling("2022-04-29", "Hideaway now causes you to put the rest of the cards on the bottom of your library in a random order instead of any order.")
    }
}
