package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pinnacle Monk {3}{R}{R} // Mystic Peak
 * Creature — Djinn Monk 2/2
 * Prowess
 * When this creature enters, return target instant or sorcery card from your graveyard to your hand.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {R}.
 */
private val PinnacleMonkFront = card("Pinnacle Monk") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Djinn Monk"
    power = 2
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature enters, return target instant or sorcery card from your graveyard to your hand."

    prowess()

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.InstantOrSorceryInGraveyard.ownedByYou())
        effect = Effects.Move(target = t, destination = Zone.HAND)
        description = "When this creature enters, return target instant or sorcery card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Jason A. Engle"
        flavorText = "Narset brought the Jeskai djinn ancient wisdom and the promise to challenge the status quo."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24d4f26e-7f96-4b38-867e-4fac819b2679.jpg?1783911228"
    }
}

private val MysticPeakBack = card("Mystic Peak") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {R}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Jason A. Engle"
        flavorText = "\"Fear not the fall. Fear never reaching the pinnacle.\"\n—Narset"
        imageUri = "https://cards.scryfall.io/normal/back/2/4/24d4f26e-7f96-4b38-867e-4fac819b2679.jpg?1783911228"
    }
}

val PinnacleMonk: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = PinnacleMonkFront,
    backFace = MysticPeakBack,
)
