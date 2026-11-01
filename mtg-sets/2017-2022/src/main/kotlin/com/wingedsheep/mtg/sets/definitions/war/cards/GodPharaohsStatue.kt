package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * God-Pharaoh's Statue — War of the Spark #238
 * {6} · Legendary Artifact
 *
 * Spells your opponents cast cost {2} more to cast.
 * At the beginning of your end step, each opponent loses 1 life.
 */
val GodPharaohsStatue = card("God-Pharaoh's Statue") {
    manaCost = "{6}"
    typeLine = "Legendary Artifact"
    oracleText = "Spells your opponents cast cost {2} more to cast.\n" +
        "At the beginning of your end step, each opponent loses 1 life."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "At the beginning of your end step, each opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Igor Kieryluk"
        flavorText = "A dictator's infuriating smirk, an affront to everything Ravnica stood for."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dce06ba-c1e1-45ec-82a7-fc10b0fa8870.jpg?1783933372"
    }
}
