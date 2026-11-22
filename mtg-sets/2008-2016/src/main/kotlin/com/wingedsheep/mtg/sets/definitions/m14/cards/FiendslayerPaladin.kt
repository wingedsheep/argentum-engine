package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fiendslayer Paladin
 * {1}{W}{W}
 * Creature — Human Knight
 * 2/2
 * First strike
 * Lifelink
 * This creature can't be the target of black or red spells your opponents control.
 *
 * The targeting restriction is a [CardsInZonesCantBeTargeted] lock over the battlefield narrowed to
 * this permanent ([StatePredicate.IsSource]), scoped to opponents' *spells* matching black or red:
 * unlike protection or hexproof from a colour it stops neither abilities (from black or red sources
 * or otherwise) nor its controller's own spells, and non-targeting black or red spells still affect
 * it (the card's ruling). It is an ability of the Paladin, so it goes away if the Paladin loses its
 * abilities, and "your opponents" follows the Paladin's current controller.
 */
val FiendslayerPaladin = card("Fiendslayer Paladin") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "First strike (This creature deals combat damage before creatures without first strike.)\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "This creature can't be the target of black or red spells your opponents control."

    keywords(Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    staticAbility {
        ability = CardsInZonesCantBeTargeted(
            zones = setOf(Zone.BATTLEFIELD),
            filter = GameObjectFilter.Any.withStatePredicate(StatePredicate.IsSource),
            sourceController = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.withAnyColor(Color.BLACK, Color.RED)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cfb0f4a-e273-4ffb-91cd-dd1a7b6f6a8f.jpg?1783939944"
        ruling(
            "2013-07-01",
            "Fiendslayer Paladin can be affected by black or red spells that don't target it. For example, " +
                "a red spell that \"deals 3 damage to each creature\" would deal damage to Fiendslayer Paladin."
        )
    }
}
