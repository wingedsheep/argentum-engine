package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Surge Engine
 * {2}
 * Artifact Creature — Construct
 * 3/2
 * Defender
 * {U}: This creature loses defender and gains "This creature can't be blocked."
 * {2}{U}: This creature becomes blue and has base power and toughness 5/4. Activate only if this
 * creature doesn't have defender.
 * {4}{U}{U}: Draw three cards. Activate only if this creature is blue and only once.
 *
 * None of the three changes has a duration, so each is `Duration.Permanent` (until the object
 * leaves the battlefield). The "can't be blocked" ability is the unconditional creature evasion
 * flag ([AbilityFlag.CANT_BE_BLOCKED]). Both activation gates read the source's *current* projected
 * characteristics, so per the rulings losing defender or becoming blue by any other means also
 * opens them. "Only once" is [ActivationRestriction.Once] — once per object, so a Surge Engine that
 * leaves and returns may draw again.
 */
val SurgeEngine = card("Surge Engine") {
    manaCost = "{2}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 2
    oracleText = "Defender\n" +
        "{U}: This creature loses defender and gains \"This creature can't be blocked.\"\n" +
        "{2}{U}: This creature becomes blue and has base power and toughness 5/4. Activate only if this " +
        "creature doesn't have defender.\n" +
        "{4}{U}{U}: Draw three cards. Activate only if this creature is blue and only once."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.RemoveKeyword(Keyword.DEFENDER, EffectTarget.Self, Duration.Permanent) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self, Duration.Permanent)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.ChangeColor(EffectTarget.Self, setOf(Color.BLUE), Duration.Permanent) then
            Effects.SetBasePowerAndToughness(5, 4, EffectTarget.Self, Duration.Permanent)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.Not(Conditions.SourceHasKeyword(Keyword.DEFENDER)))
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{U}{U}")
        effect = Effects.DrawCards(3)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.SourceMatches(GameObjectFilter.Any.withColor(Color.BLUE))
            ),
            ActivationRestriction.Once
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "81"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12b9c9c5-e992-4f1c-a779-124176c3aade.jpg?1783920094"
        ruling(
            "2022-10-14",
            "If Surge Engine loses defender in some way other than its first ability, its second ability may " +
                "still be activated. Likewise, if it becomes blue in some way other than its second ability, its " +
                "last ability may still be activated."
        )
        ruling("2022-10-14", "Surge Engine's last ability can be activated only once per game, not once per turn.")
        ruling(
            "2022-10-14",
            "If a Surge Engine whose last ability has been activated leaves the battlefield and comes back, it's " +
                "a new object on the battlefield and its last ability can be activated again (once it's blue, of course)."
        )
    }
}
