package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ib Halfheart, Goblin Tactician
 * {3}{R}
 * Legendary Creature — Goblin Advisor
 * 3/2
 * Whenever another Goblin you control becomes blocked, sacrifice it. If you do, it deals 4 damage
 * to each creature blocking it.
 * Sacrifice two Mountains: Create two 1/1 red Goblin creature tokens.
 *
 * "Each creature blocking it" has to survive the sacrifice that precedes it: once the Goblin
 * leaves the battlefield it leaves combat and its blockers stop blocking it. So the blockers are
 * gathered first ([GameObjectFilter.blockingEntity] over the triggering Goblin), then the Goblin
 * is sacrificed, and only if it actually was does the remembered group take 4 damage — dealt by
 * the sacrificed Goblin, not by Ib.
 */
val IbHalfheartGoblinTactician = card("Ib Halfheart, Goblin Tactician") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Advisor"
    power = 3
    toughness = 2
    oracleText = "Whenever another Goblin you control becomes blocked, sacrifice it. If you do, it deals 4 damage " +
        "to each creature blocking it.\nSacrifice two Mountains: Create two 1/1 red Goblin creature tokens."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN).youControl()).becomesBlocked()
        effect = Effects.Pipeline {
            val blockers = gather(GameObjectFilter.Creature.blockingEntity(EffectTarget.TriggeringEntity))
            run(
                Effects.IfYouDo(
                    action = Effects.SacrificeTarget(EffectTarget.TriggeringEntity),
                    then = Effects.ForEachInCollection(
                        blockers,
                        Effects.DealDamage(4, EffectTarget.IterationEntity, damageSource = EffectTarget.TriggeringEntity)
                    ),
                    successCriterion = SuccessCriterion.PermanentsSacrificed,
                )
            )
        }
        description = "Whenever another Goblin you control becomes blocked, sacrifice it. If you do, it deals " +
            "4 damage to each creature blocking it."
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(2, GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/e/2/e265ca24-96c0-4654-a8f3-bbffe288970a.jpg?1742506636"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Wayne Reynolds"
        flavorText = "\"Everybody but me—CHARGE!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38134389-b471-4f58-a9ae-26bf9dc1557a.jpg?1783943220"
        ruling(
            "2006-09-25",
            "If you don't actually sacrifice the Goblin (because it was removed from the battlefield before the " +
                "ability resolved, for example), no damage is dealt."
        )
    }
}
