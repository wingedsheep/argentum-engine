package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hellspur Posse Boss
 * {2}{R}{R}
 * Creature — Lizard Rogue
 * 2/4
 *
 * Other outlaws you control have haste. (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are
 * outlaws.)
 * When this creature enters, create two 1/1 red Mercenary creature tokens with "{T}: Target
 * creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 *
 * "Outlaws" are creatures with any of the [Subtype.OUTLAW_TYPES] (Assassin / Mercenary / Pirate /
 * Rogue / Warlock; per the 2024-04-12 ruling). The haste grant uses `excludeSelf` so the Boss
 * (itself a Rogue) does not grant haste to itself.
 */
val HellspurPosseBoss = card("Hellspur Posse Boss") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Rogue"
    power = 2
    toughness = 4
    oracleText = "Other outlaws you control have haste. (Assassins, Mercenaries, Pirates, Rogues, " +
        "and Warlocks are outlaws.)\n" +
        "When this creature enters, create two 1/1 red Mercenary creature tokens with \"{T}: " +
        "Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\""

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(
                GameObjectFilter.Creature.withAnyOfSubtypes(Subtype.OUTLAW_TYPES).youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(2),
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Mercenary"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = AbilityCost.Tap,
                    effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.CreatureYouControl),
                    timing = TimingRule.SorcerySpeed
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319"
        )
        description = "When this creature enters, create two 1/1 red Mercenary creature tokens " +
            "with \"{T}: Target creature you control gets +1/+0 until end of turn. Activate only " +
            "as a sorcery.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f348c7e-7d72-40f1-a65f-ee7ff4b09412.jpg?1712355772"

        ruling("2024-04-12", "A card, spell, or permanent is an outlaw if it has the Assassin, " +
            "Mercenary, Pirate, Rogue, or Warlock creature type. It doesn't matter if it has more " +
            "than one of those creature types; as long as it has at least one, it's an outlaw.")
    }
}
