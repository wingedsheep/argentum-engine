package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Illuna, Apex of Wishes — Ikoria: Lair of Behemoths #190
 * {2}{G}{U}{R} · Legendary Creature — Beast Elemental Dinosaur 6/6
 *
 * Mutate {3}{R/G}{U}{U}
 * Flying, trample
 * Whenever this creature mutates, exile cards from the top of your library until you exile a
 * nonland permanent card. Put that card onto the battlefield or into your hand.
 *
 * Every walked card is exiled face up; the found card then goes onto the battlefield (a "may" whose
 * decline branch is the hand — the choice is made after seeing the card, per the 2020-04-17
 * ruling). Lands, instants and sorceries exiled along the way stay in exile. If the library holds
 * no nonland permanent card, everything is exiled and there is no choice to make.
 */
val IllunaApexOfWishes = card("Illuna, Apex of Wishes") {
    manaCost = "{2}{G}{U}{R}"
    colorIdentity = "GRU"
    typeLine = "Legendary Creature — Beast Elemental Dinosaur"
    power = 6
    toughness = 6
    oracleText = "Mutate {3}{R/G}{U}{U} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flying, trample\n" +
        "Whenever this creature mutates, exile cards from the top of your library until you exile a nonland permanent card. Put that card onto the battlefield or into your hand."

    keywordAbility(KeywordAbility.mutate("{3}{R/G}{U}{U}"))
    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.Pipeline {
            val (found, exiled) = gatherUntilMatch(GameObjectFilter.NonlandPermanent)
            exile(exiled)
            ifNotEmpty(found) {
                run(
                    Effects.May(
                        Effects.Pipeline {
                            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                        },
                        otherwise = Effects.Pipeline { toHand(found) },
                        prompt = "Put the exiled card onto the battlefield? (Otherwise, into your hand.)"
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "190"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fc99c81-d112-4811-9bba-77a14d904692.jpg?1783931024"
        ruling("2020-04-17", "You choose whether to put the card into your hand or onto the battlefield after seeing what the card is.")
        ruling("2020-04-17", "Any land, instant, and sorcery cards exiled this way remain in exile.")
    }
}
