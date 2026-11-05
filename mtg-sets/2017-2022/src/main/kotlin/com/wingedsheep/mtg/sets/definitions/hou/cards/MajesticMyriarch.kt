package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.times

/**
 * Majestic Myriarch — Hour of Devastation #122
 * {4}{G} · Creature — Chimera · * / *
 *
 * The Odric, Lunarch Marshal shape narrowed to the Myriarch itself: each printed keyword is an
 * independent resolution-time [Effects.If] over [Conditions.ControlCreatureWithKeyword], so the
 * gained keywords are locked in once the trigger resolves (ruling). Hexproof also carries its
 * variants — "If one of those creatures has … hexproof from white, this creature gains those
 * specific variants" — through [Effects.GrantKeywordVariantsSharedByGroup].
 */
private val MYRIARCH_KEYWORDS = listOf(
    Keyword.FLYING,
    Keyword.FIRST_STRIKE,
    Keyword.DOUBLE_STRIKE,
    Keyword.DEATHTOUCH,
    Keyword.HASTE,
    Keyword.HEXPROOF,
    Keyword.INDESTRUCTIBLE,
    Keyword.LIFELINK,
    Keyword.MENACE,
    Keyword.REACH,
    Keyword.TRAMPLE,
    Keyword.VIGILANCE,
)

private val CREATURES_YOU_CONTROL = GroupFilter(GameObjectFilter.Creature.youControl())

val MajesticMyriarch = card("Majestic Myriarch") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Chimera"
    oracleText = "Majestic Myriarch's power and toughness are each equal to twice the number of creatures you control.\n" +
        "At the beginning of each combat, this creature gains flying until end of turn if you control a creature " +
        "with flying. The same is true for first strike, double strike, deathtouch, haste, hexproof, " +
        "indestructible, lifelink, menace, reach, trample, and vigilance."

    dynamicStats(DynamicAmounts.creaturesYouControl() * 2)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Composite(
            MYRIARCH_KEYWORDS.map { keyword ->
                Effects.If(
                    condition = Conditions.ControlCreatureWithKeyword(keyword),
                    then = Effects.GrantKeyword(keyword, EffectTarget.Self, Duration.EndOfTurn),
                )
            } + Effects.GrantKeywordVariantsSharedByGroup(Keyword.HEXPROOF, CREATURES_YOU_CONTROL, EffectTarget.Self),
            descriptionOverride = "This creature gains flying until end of turn if you control a creature with " +
                "flying. The same is true for first strike, double strike, deathtouch, haste, hexproof, " +
                "indestructible, lifelink, menace, reach, trample, and vigilance.",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "122"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78d47cc7-2849-432e-9bcd-6b97023f1969.jpg?1783936018"
        ruling("2020-01-24", "If one of those creatures has one or more variants of the listed keywords (for example, hexproof from white), this creature gains those specific variants.")
        ruling("2017-07-14", "Majestic Myriarch's first ability applies in all zones. While it's on the battlefield, it counts itself, so it'll be at least 2/2.")
        ruling("2017-07-14", "Majestic Myriarch's second ability triggers at the beginning of each combat, not just combat on your turn, whether or not any creatures you control have any of the listed abilities.")
        ruling("2017-07-14", "Which abilities Majestic Myriarch gains is determined as the ability resolves. They won't change even if every other creature that has the abilities leaves the battlefield or if creatures enter the battlefield or gain abilities.")
        ruling("2017-07-14", "If a creature gains one of the listed abilities before Majestic Mryiarch's triggered ability resolves, perhaps due to another ability that triggered at the beginning of combat, Majestic Myriarch will gain that ability.")
        ruling("2017-07-14", "Multiple instances of any of the abilities Majestic Myriarch can gain are redundant.")
    }
}
