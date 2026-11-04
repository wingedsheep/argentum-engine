package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dire Fleet Daredevil
 * {1}{R}
 * Creature — Human Pirate
 * 2/1
 * First strike
 * When this creature enters, exile target instant or sorcery card from an opponent's graveyard.
 * You may cast it this turn, and mana of any type can be spent to cast that spell. If that spell
 * would be put into a graveyard, exile it instead.
 *
 * Same shape as Quistis Trepe, narrowed to an opponent's graveyard: the target is exiled, then
 * gets an end-of-turn cast permission with `withAnyManaType` and an exile-instead-of-graveyard
 * rider. Normal timing rules still apply to the exiled card.
 */
val DireFleetDaredevil = card("Dire Fleet Daredevil") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 1
    oracleText = "First strike\n" +
        "When this creature enters, exile target instant or sorcery card from an opponent's " +
        "graveyard. You may cast it this turn, and mana of any type can be spent to cast that " +
        "spell. If that spell would be put into a graveyard, exile it instead."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        val target = target(TargetFilter.InstantOrSorceryInGraveyard.ownedByOpponent())
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Effects.Move(target, Zone.EXILE))
            val stolen = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(
                from = stolen,
                expiry = MayPlayExpiry.EndOfTurn,
                withAnyManaType = true,
                insteadOfGraveyard = AfterResolveDestination.EXILE,
            ))
        }
        description = "When this creature enters, exile target instant or sorcery card from an " +
            "opponent's graveyard. You may cast it this turn, and mana of any type can be spent to " +
            "cast that spell. If that spell would be put into a graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13e3c0c5-7451-4da9-acc0-d6e4e1f2655e.jpg?1783935299"

        ruling("2018-01-19", "Casting an exiled card causes it to leave exile. You can't cast it " +
            "multiple times.")
        ruling("2018-01-19", "The effect of Dire Fleet Daredevil doesn't change when you can cast " +
            "the exiled card. For example, if you exile a sorcery card, you can cast it only during " +
            "your main phase when the stack is empty.")
    }
}
