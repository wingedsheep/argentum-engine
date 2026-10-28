package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deadpool, Trading Card — {2}{B}{R} Legendary Creature — Mutant Mercenary Hero 5/3
 *
 * As Deadpool enters, you may exchange his text box and another creature's.
 * At the beginning of your upkeep, you lose 3 life.
 * {3}, Sacrifice this creature: Each other player draws a card.
 *
 * The exchange is part of the entry (an [OnEnterRun] replacement), so it happens before any
 * "when this enters" trigger is checked: choosing nothing keeps Deadpool's own text. Once
 * exchanged, the other creature carries the upkeep life loss and the sacrifice ability, and
 * Deadpool has that creature's abilities — until either leaves the battlefield.
 */
val DeadpoolTradingCard = card("Deadpool, Trading Card") {
    manaCost = "{2}{B}{R}"
    typeLine = "Legendary Creature — Mutant Mercenary Hero"
    power = 5
    toughness = 3
    oracleText = "As Deadpool enters, you may exchange his text box and another creature's.\n" +
        "At the beginning of your upkeep, you lose 3 life.\n" +
        "{3}, Sacrifice this creature: Each other player draws a card."

    replacementEffect(
        OnEnterRun(
            Effects.Pipeline {
                val creatures = gather(GameObjectFilter.Creature, excludeSelf = true)
                val chosen = chooseUpTo(
                    1,
                    from = creatures,
                    useTargetingUI = true,
                    prompt = "You may exchange Deadpool's text box with another creature's",
                )
                run(Effects.ExchangeTextBoxes(EffectTarget.Self, chosen.asTarget(0)))
            }
        )
    )

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(3, EffectTarget.Controller)
        description = "At the beginning of your upkeep, you lose 3 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.SacrificeSelf)
        effect = Effects.ForEachPlayer(Player.EachOtherPlayer, Effects.DrawCards(1, EffectTarget.Controller))
        description = "{3}, Sacrifice this creature: Each other player draws a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1753"
        artist = "Justine Cruz"
        flavorText = "\"Draw me like one of your French cards.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a14d6c5-cfd1-4860-834a-5a0dc9df0320.jpg?1783907438"
    }
}
