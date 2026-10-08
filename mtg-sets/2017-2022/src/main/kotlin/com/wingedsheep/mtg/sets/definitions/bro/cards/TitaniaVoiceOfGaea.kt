package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Titania, Voice of Gaea
 * {1}{G}{G}
 * Legendary Creature — Elemental
 * 3/4
 * Reach
 * Whenever one or more land cards are put into your graveyard from anywhere, you gain 2 life.
 * At the beginning of your upkeep, if there are four or more land cards in your graveyard and you
 * both own and control Titania, Voice of Gaea and a land named Argoth, Sanctum of Nature, exile
 * them, then meld them into Titania, Gaea Incarnate.
 *
 * The whole "if …" is an intervening if (CR 603.4): checked when the upkeep begins and again on
 * resolution. [Effects.Meld] re-checks the own-and-control half itself and does nothing if either
 * card has gone; the graveyard count is the trigger's own re-check.
 */
val TitaniaVoiceOfGaea = card("Titania, Voice of Gaea") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elemental"
    power = 3
    toughness = 4
    oracleText = "Reach\n" +
        "Whenever one or more land cards are put into your graveyard from anywhere, you gain 2 life.\n" +
        "At the beginning of your upkeep, if there are four or more land cards in your graveyard and " +
        "you both own and control Titania, Voice of Gaea and a land named Argoth, Sanctum of Nature, " +
        "exile them, then meld them into Titania, Gaea Incarnate."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Land).putIntoYourGraveyard()
        effect = Effects.GainLife(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.All(
            Conditions.CardsInGraveyardMatchingAtLeast(4, GameObjectFilter.Land),
            Conditions.SourceMatches(GameObjectFilter.Any.ownedByYou().youControl()),
            Conditions.YouControl(GameObjectFilter.Land.named(ARGOTH).ownedByYou())
        )
        effect = Effects.Meld(GameObjectFilter.Land.named(ARGOTH), into = "Titania, Gaea Incarnate")
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "193"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/deeb6a21-23b0-44f1-b70e-5899bb9d4a84.jpg?1783920039"
    }
}

private const val ARGOTH = "Argoth, Sanctum of Nature"
