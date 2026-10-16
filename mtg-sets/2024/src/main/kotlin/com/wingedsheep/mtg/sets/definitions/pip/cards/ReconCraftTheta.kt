package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS

/**
 * Recon Craft Theta
 * {4}
 * Artifact — Vehicle
 * 4/4
 *
 * Flying
 * When this Vehicle enters, create a 0/0 blue Alien creature token. Put a +1/+1 counter on it.
 * Whenever this Vehicle attacks, proliferate.
 * Crew 2
 *
 * The token and its counter arrive within one resolution: the token creator publishes the
 * `CREATED_TOKENS` collection and the counter lands on exactly that Alien before state-based
 * actions are next checked, so the 0/0 never dies in between.
 */
val ReconCraftTheta = card("Recon Craft Theta") {
    manaCost = "{4}"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this Vehicle enters, create a 0/0 blue Alien creature token. Put a +1/+1 counter on it.\n" +
        "Whenever this Vehicle attacks, proliferate. (Choose any number of permanents and/or players, " +
        "then give each another counter of each kind already there.)\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: " +
        "This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Alien"),
            imageUri = "https://cards.scryfall.io/normal/front/c/d/cd66f10d-6fc4-4510-818c-96d29b425052.jpg?1783912045",
        ) then Effects.AddCountersToCollection(CREATED_TOKENS, CounterType.PLUS_ONE_PLUS_ONE, 1)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Proliferate()
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21aa59d3-e786-4ba0-9802-01b57597cda2.jpg?1783912376"
    }
}
