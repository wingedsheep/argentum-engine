package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Purphoros, Bronze-Blooded — Theros Beyond Death #150
 * {4}{R} · Legendary Enchantment Creature — God · 7/6
 *
 * Indestructible
 * As long as your devotion to red is less than five, Purphoros isn't a creature.
 * Other creatures you control have haste.
 * {2}{R}: You may put a red creature card or an artifact creature card from your hand onto the
 * battlefield. Sacrifice it at the beginning of the next end step.
 *
 * The god clause is Purphoros, God of the Forge's devotion-gated [RemoveCardType]. The haste lord is
 * Garna, the Bloodflame's `GrantKeyword` over other creatures you control. The activation is
 * Through the Breach's hand → battlefield pipeline (an optional `chooseUpTo(1)` over red creature
 * cards and artifact creature cards) with a delayed end-step sacrifice of exactly the creature put
 * onto the battlefield — no haste grant of its own, which the static lord already supplies.
 */
val PurphorosBronzeBlooded = card("Purphoros, Bronze-Blooded") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment Creature — God"
    power = 7
    toughness = 6
    oracleText = "Indestructible\n" +
        "As long as your devotion to red is less than five, Purphoros isn't a creature.\n" +
        "Other creatures you control have haste.\n" +
        "{2}{R}: You may put a red creature card or an artifact creature card from your hand onto the " +
        "battlefield. Sacrifice it at the beginning of the next end step."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.RED), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.Pipeline {
            val candidates = gather(CardSource.FromZone(
                Zone.HAND,
                Player.You,
                GameObjectFilter.Creature.withColor(Color.RED) or GameObjectFilter.ArtifactCreature,
            ))
            val putting = chooseUpTo(1, from = candidates)
            move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            ifNotEmpty(putting) {
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(putting.asTarget)
                ))
            }
        }
        description = "{2}{R}: You may put a red creature card or an artifact creature card from your hand " +
            "onto the battlefield. Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "150"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af3af5c4-0960-44a2-976e-abdf7803c436.jpg?1783931548"
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The abilities of Gods function as long as they're on the battlefield, regardless of whether they're creatures."
        )
    }
}
