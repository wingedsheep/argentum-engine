package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nissa, Genesis Mage — Hour of Devastation #200
 * {5}{G}{G} · Legendary Planeswalker — Nissa · Loyalty 5
 *
 * +2: Untap up to two target creatures and up to two target lands.
 * −3: Target creature gets +5/+5 until end of turn.
 * −10: Look at the top ten cards of your library. You may put any number of creature and/or land
 *      cards from among them onto the battlefield. Put the rest on the bottom of your library in a
 *      random order.
 *
 * The +2 declares two independent "up to two" requirements (any of the four may be skipped, per
 * the ruling) and untaps whatever was chosen via [CardSource.ChosenTargets] — the Teferi, Who
 * Slows the Sunset shape. The −10 is the Gishath split: the cards are looked at, not revealed;
 * the chosen creature/land cards move to the battlefield together in one move, so they enter
 * simultaneously and see each other, as the second ruling requires.
 */
val NissaGenesisMage = card("Nissa, Genesis Mage") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 5
    oracleText = "+2: Untap up to two target creatures and up to two target lands.\n" +
        "−3: Target creature gets +5/+5 until end of turn.\n" +
        "−10: Look at the top ten cards of your library. You may put any number of creature and/or " +
        "land cards from among them onto the battlefield. Put the rest on the bottom of your " +
        "library in a random order."

    // +2: Untap up to two target creatures and up to two target lands.
    loyaltyAbility(+2) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        targets(TargetFilter.Land, count = 2, optional = true)
        effect = Effects.Pipeline(
            descriptionOverride = "Untap up to two target creatures and up to two target lands."
        ) {
            val chosen = gather(CardSource.ChosenTargets)
            run(Effects.TapCollection(chosen, tap = false))
        }
    }

    // −3: Target creature gets +5/+5 until end of turn.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(5, 5, creature)
    }

    // −10: Look at the top ten cards of your library. You may put any number of creature and/or
    // land cards from among them onto the battlefield. Put the rest on the bottom in a random order.
    loyaltyAbility(-10) {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(10, Player.You))
            val (toBattlefield, rest) = chooseAnyNumberSplit(
                from = looked,
                filter = GameObjectFilter.Creature or GameObjectFilter.Land,
                showAllCards = true,
                prompt = "Put any number of creature and/or land cards onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put on the bottom of your library"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "200"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2923c85a-4022-4cda-bcbb-bb000137f64f.jpg?1783935987"
        inBooster = false
        ruling(
            "2017-07-14",
            "You can activate Nissa's first ability with fewer than four targets. For example, you " +
                "could target one creature and two lands, or no creatures and two lands. You could " +
                "even choose no targets at all.",
        )
        ruling(
            "2017-07-14",
            "While resolving Nissa's last ability, all creatures and lands put onto the battlefield " +
                "this way enter at the same time. If any have triggered abilities that trigger on " +
                "something else entering the battlefield, they'll see each other.",
        )
    }
}
