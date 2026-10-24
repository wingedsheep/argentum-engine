package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thunderous Orator
 * {1}{W}
 * Creature — Kor Wizard
 * 2/2
 *
 * Vigilance
 * Whenever this creature attacks, it gains flying until end of turn if you control a creature
 * with flying. The same is true for first strike, double strike, deathtouch, indestructible,
 * lifelink, menace, and trample.
 *
 * The Odric, Lunarch Marshal shape narrowed to the Orator itself: each printed keyword is an
 * independent resolution-time [Effects.If] over [Conditions.ControlCreatureWithKeyword], so the
 * gained keywords are locked in once the trigger resolves (ruling: "Once Thunderous Orator gains
 * an ability, it doesn't matter what happens to other creatures you control with that ability").
 */
private val ORATOR_KEYWORDS = listOf(
    Keyword.FLYING,
    Keyword.FIRST_STRIKE,
    Keyword.DOUBLE_STRIKE,
    Keyword.DEATHTOUCH,
    Keyword.INDESTRUCTIBLE,
    Keyword.LIFELINK,
    Keyword.MENACE,
    Keyword.TRAMPLE,
)

val ThunderousOrator = card("Thunderous Orator") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Wizard"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "Whenever this creature attacks, it gains flying until end of turn if you control a creature with " +
        "flying. The same is true for first strike, double strike, deathtouch, indestructible, lifelink, " +
        "menace, and trample."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Composite(
            ORATOR_KEYWORDS.map { keyword ->
                Effects.If(
                    condition = Conditions.ControlCreatureWithKeyword(keyword),
                    then = Effects.GrantKeyword(keyword, EffectTarget.Self, Duration.EndOfTurn),
                )
            },
            descriptionOverride = "This creature gains flying until end of turn if you control a creature with " +
                "flying. The same is true for first strike, double strike, deathtouch, indestructible, " +
                "lifelink, menace, and trample.",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef3b2961-559f-42b1-be13-ff3d03e62809.jpg?1783927382"
        ruling(
            "2021-04-16",
            "If you control a creature with one of the abilities listed in the last sentence, Thunderous Orator " +
                "will gain that ability, not flying.",
        )
        ruling(
            "2021-04-16",
            "Check the creatures you control as the triggered ability resolves to determine which abilities " +
                "Thunderous Orator gains. Once Thunderous Orator gains an ability, it doesn't matter what happens " +
                "to other creatures you control with that ability.",
        )
    }
}
