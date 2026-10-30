package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TotalCap

/**
 * Call of the Death-Dweller
 * {2}{B}
 * Sorcery
 * Return up to two target creature cards with total mana value 3 or less from your graveyard to
 * the battlefield. Put a deathtouch counter on either of them. Then put a menace counter on either
 * of them.
 *
 * The summed-mana-value cap is a targeting restriction (Patch Up shape). "Either of them" is a
 * resolution-time choice among the creatures that actually returned (`moveTracked`): the deathtouch
 * counter goes on one chosen returned creature, then the menace counter on one chosen returned
 * creature — the same one or the other. A lone returned creature takes both; if none return, no
 * counters are placed (rulings).
 */
val CallOfTheDeathDweller = card("Call of the Death-Dweller") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creature cards with total mana value 3 or less from your graveyard to the battlefield. " +
        "Put a deathtouch counter on either of them. Then put a menace counter on either of them."

    spell {
        targets(
            TargetFilter.CreatureInYourGraveyard,
            count = 2,
            optional = true,
            totalAtMost = TotalCap.manaValue(DynamicAmounts.fixed(3)),
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val returned = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            val deathtouchHost = chooseExactly(
                1,
                from = returned,
                prompt = "Choose a creature to put a deathtouch counter on",
                useTargetingUI = true
            )
            run(Effects.AddCountersToCollection(deathtouchHost, CounterType.DEATHTOUCH))
            val menaceHost = chooseExactly(
                1,
                from = returned,
                prompt = "Choose a creature to put a menace counter on",
                useTargetingUI = true
            )
            run(Effects.AddCountersToCollection(menaceHost, CounterType.MENACE))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Vincent Proce"
        flavorText = "The living fear Nethroi's whispers. The dead heed them."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37e34017-0aeb-4a93-9edf-596bf3597a0e.jpg?1783931064"
        ruling("2020-04-17", "If a card in a player's graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "You could target a single creature card with Call of the Death-Dweller. In this case, you'll put both counters on that creature.")
        ruling("2020-04-17", "If you return two creature cards, you may put one counter on each or put both counters on one.")
        ruling("2020-04-17", "If you return no creature cards to the battlefield, you won't put any counters on any creatures.")
    }
}
