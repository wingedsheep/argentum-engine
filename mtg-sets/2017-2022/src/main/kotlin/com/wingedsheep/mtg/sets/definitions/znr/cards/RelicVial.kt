package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relic Vial — Zendikar Rising #250
 * {3} · Artifact
 *
 * {2}, {T}, Sacrifice a creature: Draw a card.
 * As long as you control a Cleric, this artifact has "Whenever a creature you control dies, each
 * opponent loses 1 life and you gain 1 life."
 *
 * Whether the Vial has its granted ability when a creature dies is decided by the game state just
 * before the death (CR 603.10a): sacrificing the only Cleric you control — to the Vial's own cost
 * or anything else — still triggers it, per the 2020-09-25 rulings.
 */
val RelicVial = card("Relic Vial") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}, Sacrifice a creature: Draw a card.\n" +
        "As long as you control a Cleric, this artifact has \"Whenever a creature you control dies, " +
        "each opponent loses 1 life and you gain 1 life.\""

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.DrawCards(1)
        description = "{2}, {T}, Sacrifice a creature: Draw a card."
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
                    effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
                        Effects.GainLife(1)
                    description = "Whenever a creature you control dies, each opponent loses 1 life and you gain 1 life."
                },
                filter = GroupFilter.source(),
            ),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Cleric")),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b9ae607-e0bc-4415-ad58-65944848bbe9.jpg?1783929311"
        ruling("2020-09-25", "Once Relic Vial's ability has triggered, that ability resolves even if you no longer control a Cleric.")
        ruling("2020-09-25", "If you sacrifice the only Cleric you control to activate Relic Vial's first ability, the triggered ability it gives itself triggers.")
        ruling("2020-09-25", "If Relic Vial leaves the battlefield at the same time as each Cleric you control, the triggered ability triggers for any creatures you control that die at the same time (possibly including those Clerics).")
        ruling("2020-09-25", "In a Two-Headed Giant game, Relic Vial's ability causes the opposing team to lose 2 life and you to gain 1 life.")
    }
}
