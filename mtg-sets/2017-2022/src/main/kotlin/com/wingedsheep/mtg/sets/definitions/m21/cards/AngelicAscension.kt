package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Ascension
 * {1}{W}
 * Instant
 * Exile target creature or planeswalker. Its controller creates a 4/4 white Angel creature token
 * with flying.
 *
 * The Crib Swap shape: exile, then a token for the target's controller. If the target is illegal
 * on resolution the spell doesn't resolve and no Angel is created (2020-06-23 ruling).
 */
val AngelicAscension = card("Angelic Ascension") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature or planeswalker. Its controller creates a 4/4 white Angel " +
        "creature token with flying."

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Exile(victim) then Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING),
            controller = EffectTarget.TargetController,
            imageUri = "https://cards.scryfall.io/normal/front/9/e/9e12d954-3ec2-46e3-b01f-1fd63159e8a4.jpg?1783930594",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Volkan Baǵa"
        flavorText = "Within her battalion she was known as the Angel of Arkvale. Until now, that was " +
            "only a metaphor."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8cca776-b0e4-4cd2-815f-36c1f86cf497.jpg?1783930747"
        ruling(
            "2020-06-23",
            "If the target permanent is an illegal target by the time Angelic Ascension tries to " +
                "resolve, the spell won't resolve. No player will create an Angel token."
        )
    }
}
