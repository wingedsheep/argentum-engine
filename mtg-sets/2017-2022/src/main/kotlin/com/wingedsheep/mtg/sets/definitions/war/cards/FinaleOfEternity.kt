package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Finale of Eternity — War of the Spark #91
 * {X}{B}{B} · Sorcery
 *
 * Destroy up to three target creatures with toughness X or less. If X is 10 or more, return all
 * creature cards from your graveyard to the battlefield.
 *
 * "Up to three target" is one optional slot of three, each target checked against toughness X or
 * less on its own (not their total). The return runs after the destroys, so a creature you own
 * that was destroyed this way comes back too, and it applies even when no target was chosen.
 */
val FinaleOfEternity = card("Finale of Eternity") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy up to three target creatures with toughness X or less. If X is 10 or more, " +
        "return all creature cards from your graveyard to the battlefield."

    spell {
        targets(TargetFilter(GameObjectFilter.Creature.toughnessAtMostX()), count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Destroy(EffectTarget.ContextTarget(0))
        ) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.xValue(),
                ComparisonOperator.GTE,
                10,
            ),
            then = Effects.Pipeline {
                val creatures = gather(
                    CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.Creature,
                    )
                )
                move(creatures, CardDestination.ToZone(Zone.BATTLEFIELD))
            },
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "91"
        artist = "Daarken"
        flavorText = "When you rule by fear, your greatest weakness is one who's no longer afraid."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85c60177-8b71-4e24-87e7-395d5e4f93ff.jpg?1783933444"
        ruling("2019-05-03", "Finale of Eternity can target up to three creatures that each have toughness X or less. That is, their total toughness doesn't have to be X or less.")
        ruling("2019-05-03", "If some but not all of the target creatures you choose are illegal targets as Finale of Eternity resolves, the legal targets are still destroyed and, if X is 10 or more, you still return cards to the battlefield.")
        ruling("2019-05-03", "If some or all of the targets are legal targets but aren't destroyed, most likely because they have indestructible, you'll still return cards to the battlefield if X is 10 or more.")
        ruling("2019-05-03", "If you target a creature you own and X is 10 or more, that card will be among those returned to the battlefield.")
        ruling("2019-05-03", "You may cast Finale of Eternity without choosing any target creatures. If X is 10 or more, you'll just return all creature cards from your graveyard to the battlefield. However, if you choose any targets and all of those targets become illegal before the spell tries to resolve, the spell won't resolve and you won't put any cards onto the battlefield.")
    }
}
