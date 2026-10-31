package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Drannith Magistrate
 * {1}{W}
 * Creature — Human Wizard
 * 1/3
 * Your opponents can't cast spells from anywhere other than their hands.
 *
 * The cast prohibition's "where" axis covering every zone but the hand. It overrides permissions
 * (flashback, foretell, escape, adventure-from-exile) and resolution-time casts alike — an
 * opponent's cascade or discover hit can't be cast from exile.
 */
val DrannithMagistrate = card("Drannith Magistrate") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    oracleText = "Your opponents can't cast spells from anywhere other than their hands."
    power = 1
    toughness = 3

    staticAbility {
        ability = PlayersCantCastSpells(Player.EachOpponent, fromZones = PlayersCantCastSpells.OUTSIDE_HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Kieran Yanner"
        flavorText = "\"Our sanctuaries must be beacons of reason and calm or else they will fall.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98b0a4a8-9319-451b-9b79-b0bca7a41e91.jpg?1783931091"
        ruling(
            "2020-04-17",
            "If an effect says that an opponent may cast a spell from anywhere other than their hand, Drannith Magistrate's restriction overrules that permission."
        )
        ruling("2020-04-17", "Effects may put cards from other zones onto the battlefield under an opponent's control.")
        ruling(
            "2020-04-17",
            "If a rule or effect allows them to do so, your opponents may play lands from zones other than their hand and activate abilities of cards in zones other than their hand."
        )
    }
}
