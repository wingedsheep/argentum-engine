package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Rarity

val RuthlessRadrat = card("Ruthless Radrat") {
    manaCost = "{1}{B}{B}"
    typeLine = "Creature — Rat Mutant"
    power = 2
    toughness = 2
    oracleText = "Squad—Exile four cards from your graveyard. (As an additional cost to cast this spell, you may pay its squad cost any number of times. When this creature enters, create that many tokens that are copies of it.)\nMenace (This creature can't be blocked except by two or more creatures.)"
    squad(additionalCost = Costs.additional.ExileCards(4))
    keywords(Keyword.MENACE)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Bruno Biazotto"
        flavorText = "They'll make you miss the mole rats."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4542e34-2905-41ce-9182-a432c8f2c451.jpg?1783912407"
        ruling("2024-03-08", "Once you announce that you’re casting Ruthless Radrat, no player may take actions until you’re done casting it. Notably, opponents can’t try to remove cards from your graveyard to stop you from paying Ruthless Radrat’s squad cost.")
        ruling("2024-03-08", "You may pay the squad cost any number of times. You will create a token that is a copy of that permanent for each time you paid the squad cost. If you paid the squad cost multiple times, the tokens will all enter the battlefield simultaneously.")
        ruling("2024-03-08", "If the spell is countered, the squad ability will not trigger, and no tokens will be created.")
        ruling("2024-03-08", "If the spell resolves but the creature with squad leaves the battlefield before its squad ability resolves, you’ll still create the token copies.")
        ruling("2024-03-08", "The tokens created by the squad ability aren’t “cast,” so any abilities that trigger when a spell is cast won’t trigger for the copies.")
        ruling("2024-03-08", "If, for some reason, the creature doesn’t have the squad ability when it’s on the battlefield, the ability won’t trigger, even if you’ve paid the squad cost one or more times.")
    }
}
