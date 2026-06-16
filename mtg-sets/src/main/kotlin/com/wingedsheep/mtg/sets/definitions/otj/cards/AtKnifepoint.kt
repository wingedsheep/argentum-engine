package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
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
 * At Knifepoint
 * {1}{B}{R}
 * Enchantment
 *
 * During your turn, outlaws you control have first strike. (Assassins, Mercenaries, Pirates,
 * Rogues, and Warlocks are outlaws.)
 * Whenever you commit a crime, create a 1/1 red Mercenary creature token with "{T}: Target
 * creature you control gets +1/+0 until end of turn. Activate only as a sorcery." This ability
 * triggers only once each turn.
 *
 * The first-strike grant is a [GrantKeyword] static over `outlaws you control`
 * ([Subtype.OUTLAW_TYPES]) gated by [Conditions.IsYourTurn] (the projection layer drops the grant
 * outside the controller's turn). The crime payoff is the standard [Triggers.YouCommitCrime]
 * triggered ability with `oncePerTurn = true`; its Mercenary token mirrors the canonical OTJ
 * Mercenary (cf. Hellspur Posse Boss): a sorcery-speed `{T}` pump of a creature you control.
 */
val AtKnifepoint = card("At Knifepoint") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment"
    oracleText = "During your turn, outlaws you control have first strike. (Assassins, Mercenaries, " +
        "Pirates, Rogues, and Warlocks are outlaws.)\n" +
        "Whenever you commit a crime, create a 1/1 red Mercenary creature token with \"{T}: Target " +
        "creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\" This " +
        "ability triggers only once each turn."

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(
            Keyword.FIRST_STRIKE,
            GroupFilter(
                GameObjectFilter.Creature.withAnyOfSubtypes(Subtype.OUTLAW_TYPES).youControl()
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(1),
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
        description = "Whenever you commit a crime, create a 1/1 red Mercenary creature token with " +
            "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a " +
            "sorcery.\" This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/897d594d-b5b0-43dc-b877-b483942416ce.jpg?1712356046"

        ruling("2024-04-12", "A card, spell, or permanent is an outlaw if it has the Assassin, " +
            "Mercenary, Pirate, Rogue, or Warlock creature type. It doesn't matter if it has more " +
            "than one of those creature types; as long as it has at least one, it's an outlaw.")
        ruling("2024-04-12", "You commit a crime as you put a spell or ability that targets an " +
            "opponent, a permanent an opponent controls, a card in an opponent's graveyard, a " +
            "spell an opponent controls, or an ability an opponent controls onto the stack.")
        ruling("2024-04-12", "At Knifepoint's last ability triggers only once each turn, no matter " +
            "how many crimes you commit that turn.")
    }
}
