package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vizkopa Guildmage — Gatecrash #206
 * {W}{B} · Creature — Human Wizard · 2 / 2
 *
 *  - The first ability is a plain until-end-of-turn lifelink grant on a target creature.
 *  - The second ability creates a global triggered ability until end of turn (False Cure's /
 *    Duskmantle Guildmage's shape), so it outlives the Guildmage. Its controller is the activator,
 *    so `Triggers.you.gainsLife()` reads "whenever *you* gain life"; "that much" is the triggering
 *    event's gain ([DynamicAmounts.triggerLifeGained], Sunbond's amount). Each activation creates
 *    its own ability, so two activations drain twice (second ruling).
 */
val VizkopaGuildmage = card("Vizkopa Guildmage") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{1}{W}{B}: Target creature gains lifelink until end of turn.\n" +
        "{1}{W}{B}: Whenever you gain life this turn, each opponent loses that much life."

    activatedAbility {
        cost = Costs.Mana("{1}{W}{B}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.LIFELINK, creature, Duration.EndOfTurn)
        description = "Target creature gains lifelink until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}{B}")
        effect = Effects.CreateGlobalTriggeredAbility(
            duration = Duration.EndOfTurn,
            ability = TriggeredAbility.create(
                trigger = Triggers.you.gainsLife(),
                effect = Effects.LoseLife(
                    amount = DynamicAmounts.triggerLifeGained(),
                    target = EffectTarget.PlayerRef(Player.EachOpponent)
                ),
                descriptionOverride = "Whenever you gain life this turn, each opponent loses that much life."
            )
        )
        description = "Whenever you gain life this turn, each opponent loses that much life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41f505a3-2c54-43a9-b4a9-43a1451b36f6.jpg?1783940099"
        ruling("2017-11-17", "Multiple instances of lifelink on the same creature are redundant.")
        ruling(
            "2017-11-17",
            "Each time Vizkopa Guildmage's second ability resolves, a delayed triggered ability is created. " +
                "Whenever you gain life that turn, each of those abilities will trigger. For example, if you " +
                "activate the second ability twice, let those abilities resolve, then gain 2 life, each " +
                "opponent will lose a total of 4 life."
        )
        ruling(
            "2017-11-17",
            "In a Two-Headed Giant game, Vizkopa Guildmage's delayed triggered ability causes the opposing " +
                "team to lose twice the life you gained."
        )
    }
}
