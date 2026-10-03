package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Infernal Captor
 * {3}{R}
 * Creature — Devil Rogue
 * 3/3
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, gain control of target artifact or creature until end
 * of turn. Untap that permanent. It gains haste until end of turn.
 *
 * The targeted payoff rides the exploit reflexive, so the target is chosen after the sacrifice
 * and the payoff still happens if Infernal Captor exploits itself.
 */
val InfernalCaptor = card("Infernal Captor") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil Rogue"
    power = 3
    toughness = 3
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, gain control of target artifact or creature until end of turn. " +
        "Untap that permanent. It gains haste until end of turn."

    exploit {
        val t = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Josu Hernaiz"
        flavorText = "Possession is nine-tenths of the law. His fist is the other tenth."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b30243d-511b-47b8-bbed-0395d3de903d.jpg?1783911270"
    }
}
