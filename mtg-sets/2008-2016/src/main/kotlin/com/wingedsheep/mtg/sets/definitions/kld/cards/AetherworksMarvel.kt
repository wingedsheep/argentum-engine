package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aetherworks Marvel — Kaladesh #193
 * {4} Legendary Artifact
 *
 * Whenever a permanent you control is put into a graveyard, you get {E} (an energy counter).
 * {T}, Pay six {E}: Look at the top six cards of your library. You may cast a spell from among
 * them without paying its mana cost. Put the rest on the bottom of your library in a random order.
 *
 *  - The energy trigger is Tablet of Epityr's "a permanent you control dies" (ANY binding, so it
 *    also sees Aetherworks Marvel itself and tokens on their way out).
 *  - The activation is Svella, Ice Shaper's look-and-cast pipeline: a free cast of one *spell*
 *    (lands aren't spells) during resolution, then every looked-at card still in the library goes
 *    to the bottom in a random order.
 */
val AetherworksMarvel = card("Aetherworks Marvel") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Whenever a permanent you control is put into a graveyard, you get {E} (an energy counter).\n" +
        "{T}, Pay six {E}: Look at the top six cards of your library. You may cast a spell from among " +
        "them without paying its mana cost. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).dies()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 6))
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6, Player.You))
            val chosen = chooseSpell(
                from = looked,
                showAllCards = true,
                prompt = "You may cast a spell from among them without paying its mana cost.",
                selectedLabel = "Cast for free",
                remainderLabel = "Put on bottom",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
            val notCast = filter(looked, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            toLibraryBottom(notCast, order = CardOrder.Random)
        }
        description = "{T}, Pay six {E}: Look at the top six cards of your library. You may cast a spell " +
            "from among them without paying its mana cost. Put the rest on the bottom of your library " +
            "in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "193"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/884f6948-3e03-48c6-8be2-6f2539386c9d.jpg?1783937164"
        ruling("2016-09-20", "Aetherworks Marvel's first ability triggers whenever any permanent you control is put into a graveyard from the battlefield, including Aetherworks Marvel itself and other permanents put into a graveyard at the same time as it.")
        ruling("2016-09-20", "Tokens that are sacrificed or destroyed are put into their owner's graveyard before ceasing to exist. If you controlled the token, Aetherworks Marvel's first ability will trigger.")
        ruling("2016-09-20", "The card cast with Aetherworks Marvel's second ability is cast from your library.")
        ruling("2016-09-20", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs, such as emerge costs. You can, however, pay additional costs. If the card has any mandatory additional costs, such as that of Incendiary Sabotage, you must pay those to cast the card.")
        ruling("2016-09-20", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
