package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adverse Conditions
 * {3}{U}
 * Instant
 * Devoid (This card has no color.)
 * Tap up to two target creatures. Those creatures don't untap during their controller's next
 * untap step. Create a 1/1 colorless Eldrazi Scion creature token. It has "Sacrifice this token:
 * Add {C}."
 *
 * The tap-and-freeze half is Frost Breath's shape: each target is tapped and given
 * [AbilityFlag.DOESNT_UNTAP] until after its *current* controller's next untap step (the
 * duration follows the creature, not the controller, as the ruling requires). Cast with no
 * targets, the spell still resolves and makes the Scion; cast with targets that all become
 * illegal, it fizzles and makes nothing. The Scion token is Call the Scions' inline shape.
 */
val AdverseConditions = card("Adverse Conditions") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Tap up to two target creatures. Those creatures don't untap during their controller's " +
        "next untap step. Create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        ) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.SacrificeSelf,
                    effect = Effects.AddColorlessMana(1),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "Sacrifice this token: Add {C}.",
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1783938121",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03d86e0b-294f-41b8-a5cd-d3144a019334.jpg?1783938214"
        ruling(
            "2015-08-25",
            "You can cast Adverse Conditions with no targets. When it resolves, you'll get an Eldrazi " +
                "Scion. However, if you cast Adverse Conditions with any targets and all of those targets " +
                "are illegal as it tries to resolve, it won't resolve and none of its effects will happen. " +
                "You won't get the Eldrazi Scion in that case."
        )
        ruling(
            "2015-08-25",
            "Adverse Conditions tracks the creatures, but not their controller. If either of the " +
                "creatures changes controllers before its first controller's next untap step has come " +
                "around, then it won't untap during its new controller's next untap step."
        )
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
