package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Talent of the Telepath
 * {2}{U}{U}
 * Sorcery
 *
 * Target opponent reveals the top seven cards of their library. You may cast an instant or
 * sorcery spell from among them without paying its mana cost. Then that player puts the rest
 * into their graveyard.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you
 * may cast up to two instant and/or sorcery spells from among the revealed cards instead of one.
 *
 * Spell mastery is checked once, as the spell starts resolving (Talent itself is still on the
 * stack, so it never counts) and picks between a one-cast and a two-cast resolution. The casts
 * happen one at a time straight out of the opponent's library (Velomachus Lorehold's
 * choose-then-[Effects.CastFromCollectionWithoutPayingCost] step): you cast the first, then may
 * pick a second from the revealed instants and sorceries still in the library, so the one cast
 * last resolves first. "The rest" is every revealed card still in the library afterwards —
 * including a chosen card that couldn't be cast for want of a legal target.
 */
val TalentOfTheTelepath = card("Talent of the Telepath") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals the top seven cards of their library. You may cast an instant " +
        "or sorcery spell from among them without paying its mana cost. Then that player puts the rest " +
        "into their graveyard.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you may " +
        "cast up to two instant and/or sorcery spells from among the revealed cards instead of one."

    spell {
        target(Targets.Opponent)
        effect = Effects.If(
            condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
            then = revealAndCast(casts = 2),
            otherwise = revealAndCast(casts = 1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5da13a1-8be2-4312-a660-f3e102c552b6.jpg?1783938346"
        ruling("2015-06-22", "If the spell mastery ability applies, you'll cast the two cards in order. The one you cast last will be the one that resolves first.")
        ruling("2015-06-22", "You cast the instant and/or sorcery card(s) from your opponent's library as Talent of the Telepath is resolving. Ignore timing restrictions based on the card's type. Other timing restrictions, such as \"Cast [this card] only during combat,\" must be followed.")
        ruling("2015-06-22", "The cards will be put into their owner's graveyard after they resolve, not yours.")
        ruling("2015-06-22", "If you can't cast any instant or sorcery cards (perhaps because there are no legal targets available) or if you choose not to cast one, then Talent of the Telepath finishes resolving. Any of the revealed cards you didn't cast will be put into that player's graveyard.")
        ruling("2015-06-22", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can pay additional costs such as kicker costs. If the card has mandatory additional costs, you must pay those.")
        ruling("2015-06-22", "If the card has {X} in its mana cost, you must choose 0 as its value.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}

/**
 * The target opponent reveals their top seven; you may cast up to [casts] instant and/or sorcery
 * spells from among them, one at a time, without paying their mana costs; then every revealed card
 * still in their library goes to their graveyard.
 */
private fun revealAndCast(casts: Int): Effect = Effects.Pipeline {
    val revealed = gather(CardSource.TopOfLibrary(7, Player.TargetOpponent), revealed = true)
    repeat(casts) { index ->
        val stillRevealed = filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
        val chosen = chooseUpTo(
            1,
            from = stillRevealed,
            filter = GameObjectFilter.InstantOrSorcery,
            showAllCards = true,
            prompt = if (index == 0) {
                "You may cast an instant or sorcery spell from among the revealed cards without paying its mana cost"
            } else {
                "Spell mastery: you may cast another instant or sorcery spell from among the revealed cards " +
                    "without paying its mana cost"
            },
            selectedLabel = "Cast for free",
            remainderLabel = "Put into graveyard",
        )
        run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
    }
    val rest = filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
    toGraveyard(rest, Player.TargetOpponent)
}
