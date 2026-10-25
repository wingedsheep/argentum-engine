package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Papalymo Totolymo
 * {W}{B}
 * Legendary Creature — Dwarf Wizard
 * 1/2
 * Whenever you cast a noncreature spell, Papalymo Totolymo deals 1 damage to each opponent and you
 * gain 1 life.
 * {4}, {T}, Sacrifice Papalymo Totolymo: Each opponent who lost life this turn sacrifices a
 * creature with the greatest power among creatures they control.
 *
 * Per the ruling, each qualifying opponent in turn order chooses a creature, then every chosen
 * creature is sacrificed at once. So the pipeline first records the opponents who lost life this
 * turn (inside the per-player loop `lifeLostThisTurn()` reads the iterated player), then collects
 * one greatest-power choice from each of them (Crackling Doom's `GreatestPower` narrowing), and
 * sacrifices the collected creatures after the loop.
 */
val PapalymoTotolymo = card("Papalymo Totolymo") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Dwarf Wizard"
    power = 1
    toughness = 2
    oracleText = "Whenever you cast a noncreature spell, Papalymo Totolymo deals 1 damage to each " +
        "opponent and you gain 1 life.\n" +
        "{4}, {T}, Sacrifice Papalymo Totolymo: Each opponent who lost life this turn sacrifices a " +
        "creature with the greatest power among creatures they control."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever you cast a noncreature spell, Papalymo Totolymo deals 1 damage to " +
            "each opponent and you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val losers = forEachPlayerCollecting(Player.EachOpponent) {
                listOf(
                    storePlayer(
                        onlyIf = Conditions.CompareAmounts(
                            DynamicAmounts.lifeLostThisTurn(),
                            ComparisonOperator.GTE,
                            1,
                        )
                    )
                )
            }.single()
            val chosen = forEachPlayerCollecting(losers.asPlayers) {
                val creatures = gather(GameObjectFilter.Creature, Player.You)
                val greatest = filter(creatures, CollectionFilter.GreatestPower)
                listOf(
                    chooseExactly(
                        1,
                        from = greatest,
                        useTargetingUI = true,
                        prompt = "Sacrifice a creature with the greatest power among creatures you control",
                        selectedLabel = "Sacrifice",
                    )
                )
            }.single()
            sacrifice(chosen)
        }
        description = "Each opponent who lost life this turn sacrifices a creature with the greatest " +
            "power among creatures they control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Immanuela Crovius"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b876b1e-fcf4-4e5c-81b0-1080ac8099ed.jpg?1783906343"
        ruling("2025-06-06", "While resolving Papalymo Totolymo's second ability, the next opponent in turn order who lost life this turn chooses a creature with the greatest power among creatures they control, then each other opponent who lost life this turn in turn order does the same. Then each of the chosen creatures are sacrificed simultaneously.")
        ruling("2025-06-06", "Papalymo Totolymo's first ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
    }
}
