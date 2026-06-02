package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
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
 * Evercoat Ursine
 * {4}{G}
 * Creature — Elemental Bear
 * 6/5
 *
 * Trample
 * Hideaway 3, hideaway 3 (When this creature enters, look at the top three cards of your
 * library, exile one face down, then put the rest on the bottom in a random order. Then
 * do it again.)
 * Whenever this creature deals combat damage to a player, if there are cards exiled with
 * it, you may play one of them without paying its mana cost.
 *
 * Hideaway is modeled compositionally: each "Hideaway 3" is its own ETB triggered ability
 * that gathers the top three cards, prompts the controller to pick one, exiles that pick
 * face down and linked to this permanent, then bottom-randomizes the remainder. The
 * combat-damage trigger reads cards from [CardSource.FromLinkedExile] and grants free
 * cast permission — same shape as Etali, Primal Storm.
 */
val EvercoatUrsine = card("Evercoat Ursine") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Bear"
    oracleText = "Trample\n" +
        "Hideaway 3, hideaway 3 (When this creature enters, look at the top three cards " +
        "of your library, exile one face down, then put the rest on the bottom in a " +
        "random order. Then do it again.)\n" +
        "Whenever this creature deals combat damage to a player, if there are cards " +
        "exiled with it, you may play one of them without paying its mana cost."
    power = 6
    toughness = 5

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.hideaway(3))
    keywordAbility(KeywordAbility.hideaway(3))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = hideawayThree(suffix = "A")
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = hideawayThree(suffix = "B")
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
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
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Allen Douglas"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b299d768-b171-47e5-b69d-82ce13917c43.jpg?1721440993"
        ruling("2024-07-26", "\"Hideaway N\" means \"When this permanent enters, look at the top N cards of your library. Exile one of them face down and put the rest on the bottom of your library in a random order. The exiled card gains 'The player who controls the permanent that exiled this card may look at this card in the exile zone.'\"")
        ruling("2024-07-26", "Any player who has controlled a permanent with a hideaway ability since a card was exiled with it may look at that card.")
        ruling("2024-07-26", "You choose and play the card while Evercoat Ursine's last ability is resolving and still on the stack. You can't wait to play it later in the turn.")
        ruling("2024-07-26", "If one of the exiled cards is a land card, you may play it only if you have an available land play remaining this turn.")
        ruling("2024-07-26", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when playing it without paying its mana cost.")
        ruling("2024-07-26", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the spell has any mandatory additional costs, those must be paid to cast the spell.")
    }
}

/**
 * One iteration of Hideaway 3: gather top 3 of controller's library, controller picks 1
 * to exile face-down (linked to source), the remainder goes to the bottom in random
 * order. Collection names are suffixed so two instances on the same card don't collide
 * when both ETB triggers resolve sequentially.
 */
private fun hideawayThree(suffix: String): Effect = Effects.Composite(
    listOf(
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(
                count = DynamicAmount.Fixed(3),
                player = Player.You
            ),
            storeAs = "hideawayTop$suffix"
        ),
        SelectFromCollectionEffect(
            from = "hideawayTop$suffix",
            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
            storeSelected = "hideawayPicked$suffix",
            storeRemainder = "hideawayRest$suffix",
            prompt = "Choose a card to exile face down",
            selectedLabel = "Exile face down",
            remainderLabel = "Put on bottom of library"
        ),
        MoveCollectionEffect(
            from = "hideawayPicked$suffix",
            destination = CardDestination.ToZone(Zone.EXILE),
            faceDown = true,
            linkToSource = true
        ),
        MoveCollectionEffect(
            from = "hideawayRest$suffix",
            destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            order = CardOrder.Random
        )
    )
)
