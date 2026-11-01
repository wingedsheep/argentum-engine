package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mesmerizing Benthid — Ravnica Allegiance #43
 * {3}{U}{U} · Creature — Octopus · 4/5
 *
 * When this creature enters, create two 0/2 blue Illusion creature tokens with "Whenever this token
 * blocks a creature, that creature doesn't untap during its controller's next untap step."
 * This creature has hexproof as long as you control an Illusion.
 *
 * The tokens carry Cleric of Chill Depths' block trigger (a floating `DOESNT_UNTAP` grant on the
 * blocked attacker through its controller's next untap step). Hexproof is Aeronaut Tinkerer's
 * conditional-keyword shape, gated on any Illusion permanent you control — not just these tokens.
 */
val MesmerizingBenthid = card("Mesmerizing Benthid") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, create two 0/2 blue Illusion creature tokens with " +
        "\"Whenever this token blocks a creature, that creature doesn't untap during its controller's " +
        "next untap step.\"\nThis creature has hexproof as long as you control an Illusion."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Illusion"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/c/c/ccc3d8da-9005-44d9-bb10-18cdac7a32f5.jpg?1783933605",
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature),
                    effect = Effects.GrantKeyword(
                        AbilityFlag.DOESNT_UNTAP,
                        EffectTarget.TriggeringEntity,
                        Duration.UntilAfterAffectedControllersNextUntap(),
                    ),
                    descriptionOverride = "Whenever this token blocks a creature, that creature doesn't untap " +
                        "during its controller's next untap step.",
                )
            ),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.ILLUSION)),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "43"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bed7c69-1302-4c1a-b410-b4e6c4ef4c0c.jpg?1783933708"
        ruling(
            "2019-01-25",
            "Mesmerizing Benthid has hexproof as long as you control any Illusion, not just Illusion " +
                "tokens like the ones created by its first ability.",
        )
        ruling(
            "2019-01-25",
            "The triggered ability of the Illusion tokens doesn't tap the attacking creature if it's " +
                "untapped, most likely because it has vigilance.",
        )
    }
}
