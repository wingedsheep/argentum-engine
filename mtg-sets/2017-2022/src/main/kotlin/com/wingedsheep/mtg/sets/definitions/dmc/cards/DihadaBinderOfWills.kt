package com.wingedsheep.mtg.sets.definitions.dmc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dihada, Binder of Wills
 * {1}{R}{W}{B}
 * Legendary Planeswalker — Dihada
 * Loyalty 5
 * +2: Up to one target legendary creature gains vigilance, lifelink, and indestructible until your
 * next turn.
 * −3: Reveal the top four cards of your library. Put any number of legendary cards from among them
 * into your hand and the rest into your graveyard. Create a Treasure token for each card put into
 * your graveyard this way.
 * −11: Gain control of all nonland permanents until end of turn. Untap them. They gain haste until
 * end of turn.
 * Dihada, Binder of Wills can be your commander.
 *
 * - −3 counts the cards that actually reached the graveyard ([moveTracked]), so a replacement that
 *   exiles them instead (Rest in Peace) makes no Treasures.
 * - −11 is Insurrection's shape over every nonland permanent.
 * - "Can be your commander" is read off the oracle text by `CommanderEligibility`.
 */
val DihadaBinderOfWills = card("Dihada, Binder of Wills") {
    manaCost = "{1}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Planeswalker — Dihada"
    startingLoyalty = 5
    oracleText = "+2: Up to one target legendary creature gains vigilance, lifelink, and " +
        "indestructible until your next turn.\n" +
        "−3: Reveal the top four cards of your library. Put any number of legendary cards from " +
        "among them into your hand and the rest into your graveyard. Create a Treasure token for " +
        "each card put into your graveyard this way.\n" +
        "−11: Gain control of all nonland permanents until end of turn. Untap them. They gain " +
        "haste until end of turn.\n" +
        "Dihada, Binder of Wills can be your commander."

    loyaltyAbility(+2) {
        val creature = target(TargetFilter.Creature.legendary(), optional = true)
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, creature, Duration.UntilYourNextTurn) then
            Effects.GrantKeyword(Keyword.LIFELINK, creature, Duration.UntilYourNextTurn) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature, Duration.UntilYourNextTurn)
        description = "Up to one target legendary creature gains vigilance, lifelink, and " +
            "indestructible until your next turn."
    }

    loyaltyAbility(-3) {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(4), revealed = true)
            val (kept, rest) = chooseAnyNumberSplit(
                from = revealed,
                filter = GameObjectFilter.Any.legendary(),
                showAllCards = true,
                prompt = "Put any number of legendary cards into your hand",
                selectedLabel = "Put into your hand",
                remainderLabel = "Put into your graveyard",
            )
            toHand(kept)
            val milled = moveTracked(rest, CardDestination.ToZone(Zone.GRAVEYARD, Player.You))
            run(Effects.CreateTreasure(milled.count))
        }
        description = "Reveal the top four cards of your library. Put any number of legendary " +
            "cards from among them into your hand and the rest into your graveyard. Create a " +
            "Treasure token for each card put into your graveyard this way."
    }

    loyaltyAbility(-11) {
        val nonland = GroupFilter(GameObjectFilter.NonlandPermanent)
        effect = Effects.ForEachInGroup(nonland, Effects.GainControl(EffectTarget.IterationEntity, Duration.EndOfTurn)) then
            Effects.ForEachInGroup(nonland, Effects.Untap(EffectTarget.IterationEntity)) then
            Effects.ForEachInGroup(nonland, Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn))
        description = "Gain control of all nonland permanents until end of turn. Untap them. They " +
            "gain haste until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddeb54d6-a600-42b9-98df-20f8d58caed8.jpg?1783921477"
    }
}
