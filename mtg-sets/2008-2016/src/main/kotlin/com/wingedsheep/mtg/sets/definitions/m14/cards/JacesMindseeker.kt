package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace's Mindseeker
 * {4}{U}{U}
 * Creature — Fish Illusion
 * 4/4
 * Flying
 * When this creature enters, target opponent mills five cards. You may cast an instant or sorcery
 * spell from among them without paying its mana cost.
 *
 * Patient Rebuilding's targeted-opponent pipeline mill feeding Summons of Saruman's
 * choose-up-to-one cast. The choice is over the milled collection only ("from among them" — not
 * the whole graveyard), and the collection keeps tracking the cards even if a replacement such as
 * Rest in Peace sends them to exile instead, which the 2013-07-01 ruling says still lets you cast one.
 */
val JacesMindseeker = card("Jace's Mindseeker") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish Illusion"
    oracleText = "Flying\nWhen this creature enters, target opponent mills five cards. You may cast an " +
        "instant or sorcery spell from among them without paying its mana cost."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val milled = mill(5, Player.TargetOpponent)
            val castable = filter(milled, GameObjectFilter.InstantOrSorcery)
            val chosen = chooseUpTo(
                1,
                from = castable,
                selectedLabel = "Cast without paying its mana cost",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f67852a6-ae75-44e7-9e2d-d458c7b9d869.jpg?1783939933"
        ruling(
            "2013-07-01",
            "If you cast an instant or sorcery card this way, you do so while the ability of Jace's " +
                "Mindseeker is resolving. If you choose not to (or can't), you won't get a chance to cast one later."
        )
        ruling(
            "2013-07-01",
            "\"From among them\" means the five cards put into the graveyard, not all cards in that graveyard."
        )
        ruling(
            "2013-07-01",
            "If the five cards are put into a public zone such as exile instead of a graveyard (perhaps due " +
                "to the ability of Rest in Peace), you can cast one of those instant or sorcery cards from that zone."
        )
    }
}
