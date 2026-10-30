package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantEnterTheBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kunoros, Hound of Athreos
 * {1}{W}{B}
 * Legendary Creature — Dog
 * 3/3
 * Vigilance, menace, lifelink
 * Creature cards in graveyards can't enter the battlefield.
 * Players can't cast spells from graveyards.
 *
 * The two graveyard locks of Weathered Runestone / Grafdigger's Cage, scoped to the graveyard: the
 * entry prohibition is matched against the card as it is in the graveyard (so a Theros god card is
 * a creature card there whatever its controller's devotion, and Sculpting Steel can still enter as
 * a copy of a creature — the rulings), and the cast prohibition's "where" axis leaves lands played
 * from graveyards and cards cast after being exiled from one untouched.
 */
val KunorosHoundOfAthreos = card("Kunoros, Hound of Athreos") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Dog"
    power = 3
    toughness = 3
    oracleText = "Vigilance, menace, lifelink\n" +
        "Creature cards in graveyards can't enter the battlefield.\n" +
        "Players can't cast spells from graveyards."

    keywords(Keyword.VIGILANCE, Keyword.MENACE, Keyword.LIFELINK)

    staticAbility {
        ability = CantEnterTheBattlefield(GameObjectFilter.Creature, fromZones = setOf(Zone.GRAVEYARD))
    }
    staticAbility {
        ability = PlayersCantCastSpells(Player.Each, fromZones = setOf(Zone.GRAVEYARD))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Zezhou Chen"
        flavorText = "Three sharp barks announce an attempt to escape the Underworld. Three sharp bites end it."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbc0d2c3-8060-4155-b10e-d641648a4e6b.jpg?1783931520"
        ruling("2020-01-24", "Players can still play lands from graveyards if an effect allows them to do so.")
        ruling(
            "2020-01-24",
            "If an effect exiles a card from a graveyard and allows a player to cast it, that player may do so. " +
                "The spell is cast from exile, not a graveyard."
        )
        ruling(
            "2020-01-24",
            "Look at the card as it exists in your graveyard to determine whether it can enter the battlefield. " +
                "For example, Sculpting Steel (a noncreature card in the graveyard) can be put onto the " +
                "battlefield as a copy of a creature, but Phyrexian Metamorph (a creature card in the " +
                "graveyard) can't be put onto the battlefield, even if it would copy a noncreature artifact. " +
                "A Theros Beyond Death God creature card can't be put onto the battlefield regardless of your " +
                "devotion to its colors."
        )
    }
}
