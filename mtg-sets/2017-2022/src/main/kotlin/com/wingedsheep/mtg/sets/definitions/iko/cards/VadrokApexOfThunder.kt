package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vadrok, Apex of Thunder — Ikoria: Lair of Behemoths #214
 * {U}{R}{W} · Legendary Creature — Elemental Dinosaur Cat 3/3
 *
 * Mutate {1}{W/U}{R}{R}
 * Flying, first strike
 * Whenever this creature mutates, you may cast target noncreature card with mana value 3 or
 * less from your graveyard without paying its mana cost.
 *
 * The cast happens during the trigger's resolution (Torrential Gearhulk shape). A land card is a
 * legal target (noncreature, mana value 0) but can't be cast, so it is filtered out before the cast.
 */
val VadrokApexOfThunder = card("Vadrok, Apex of Thunder") {
    manaCost = "{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Elemental Dinosaur Cat"
    power = 3
    toughness = 3
    oracleText = "Mutate {1}{W/U}{R}{R} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flying, first strike\n" +
        "Whenever this creature mutates, you may cast target noncreature card with mana value 3 or less from your graveyard without paying its mana cost."

    keywordAbility(KeywordAbility.mutate("{1}{W/U}{R}{R}"))
    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        target(
            TargetFilter(
                GameObjectFilter.Noncreature.manaValueAtMost(3).ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val castable = filter(chosen, GameObjectFilter.Nonland)
            run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(from = castable)))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "214"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed7b0256-d342-4523-9516-6a007bf51825.jpg?1783931014"
        ruling("2020-04-17", "If you wish to cast the target card, you do so as part of the resolution of Vadrok's ability. You can't wait to cast it later. Timing permissions based on a card's type are ignored.")
        ruling("2020-04-17", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
