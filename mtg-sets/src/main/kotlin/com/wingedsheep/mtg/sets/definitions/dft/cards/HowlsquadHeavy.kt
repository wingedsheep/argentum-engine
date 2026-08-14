package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Howlsquad Heavy — Aetherdrift #134
 * {2}{R} · Creature — Goblin Mercenary · 2/3
 *
 * Start your engines!
 * Other Goblins you control have haste.
 * At the beginning of combat on your turn, create a 1/1 red Goblin creature token. That token
 * attacks this combat if able.
 * Max speed — {T}: Add {R} for each Goblin you control.
 */
val HowlsquadHeavy = card("Howlsquad Heavy") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Mercenary"
    power = 2
    toughness = 3
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Other Goblins you control have haste.\n" +
        "At the beginning of combat on your turn, create a 1/1 red Goblin creature token. That " +
        "token attacks this combat if able.\n" +
        "Max speed — {T}: Add {R} for each Goblin you control."

    startYourEngines()

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN).youControl(),
                excludeSelf = true,
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                imageUri = "https://cards.scryfall.io/normal/front/7/0/7072dea6-0d99-47fa-a83d-c8607e6a4bbd.jpg?1783907679",
            ),
            Effects.MarkMustAttackThisTurn(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)),
        )
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(
                Color.RED,
                // No `.youControl()` on the filter: `battlefield(Player.You, …)` already restricts
                // to permanents you control, and the redundant predicate only doubled the
                // qualifier in the generated description ("you control Goblins you control").
                DynamicAmounts.battlefield(
                    Player.You,
                    filter = GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
                ).count(),
            )
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df582f80-7b9a-4f71-95a9-70548ec7d2d7.jpg?1783907881"
        ruling(
            "2025-02-07",
            "Mana abilities don't use the stack, which means that they can't be responded to. " +
                "Notably, this means that a player can't respond to this creature's last ability " +
                "by destroying one of your Goblins."
        )
        ruling(
            "2025-02-07",
            "“Max speed — [ability]” means “As long as you have max speed, this object has " +
                "[ability].” If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
    }
}
