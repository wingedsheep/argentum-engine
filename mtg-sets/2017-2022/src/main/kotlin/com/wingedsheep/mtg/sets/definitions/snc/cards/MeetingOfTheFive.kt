package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Meeting of the Five
 * {3}{W}{U}{B}{R}{G}
 * Sorcery
 * Exile the top ten cards of your library. You may cast spells with exactly three colors from
 * among them this turn. Add {W}{W}{U}{U}{B}{B}{R}{R}{G}{G}. Spend this mana only to cast spells
 * with exactly three colors.
 *
 * "Spells with exactly three colors" is a property of the spell as it is cast, so the grant
 * carries `castColorCount = 3` (checked against the face being cast) rather than filtering the
 * exiled cards; `nonLandOnly` because "cast" never covers a land play. The ten mana is five
 * two-mana adds, each carrying [ManaRestriction.SpellsWithExactColorCount].
 */
val MeetingOfTheFive = card("Meeting of the Five") {
    manaCost = "{3}{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Sorcery"
    oracleText = "Exile the top ten cards of your library. You may cast spells with exactly three colors from among them this turn. Add {W}{W}{U}{U}{B}{B}{R}{R}{G}{G}. Spend this mana only to cast spells with exactly three colors."

    spell {
        val threeColors = ManaRestriction.SpellsWithExactColorCount(3)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(10))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(exiled, nonLandOnly = true, castColorCount = 3))
        } then Effects.AddMana(Color.WHITE, 2, restriction = threeColors) then
            Effects.AddMana(Color.BLUE, 2, restriction = threeColors) then
            Effects.AddMana(Color.BLACK, 2, restriction = threeColors) then
            Effects.AddMana(Color.RED, 2, restriction = threeColors) then
            Effects.AddMana(Color.GREEN, 2, restriction = threeColors)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "202"
        artist = "Dominik Mayer"
        flavorText = "For years, the five families ruled New Capenna in uneasy peace, and the city prospered."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6c3c08f-ae9d-4932-ba0e-65652b8b318b.jpg?1783923081"

        ruling("2022-04-29", "You may cast any spells that have exactly three colors from among the cards exiled this way, even if they aren't the same three colors. For example, you could cast a spell that is white, blue, and black, then cast a spell that is black, red, and green.")
        ruling("2022-04-29", "The mana created by Meeting of the Five can be spent on any portion of the cost for a spell with exactly three colors, including any generic cost component or additional costs it has.")
    }
}
