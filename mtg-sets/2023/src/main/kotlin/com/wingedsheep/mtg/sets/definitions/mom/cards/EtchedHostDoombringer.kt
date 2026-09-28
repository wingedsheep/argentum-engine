package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Etched Host Doombringer — {4}{B}
 * Creature — Phyrexian Demon 3/5 (common, MOM #102)
 *
 * When this creature enters, choose one —
 * • Target opponent loses 2 life and you gain 2 life.
 * • Choose target battle. If an opponent protects it, remove three defense counters from it.
 *   Otherwise, put three defense counters on it.
 *
 * The battle mode keys on the battle's protector (CR 310.9), like Portent Tracker.
 */
val EtchedHostDoombringer = card("Etched Host Doombringer") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Demon"
    power = 3
    toughness = 5
    oracleText = "When this creature enters, choose one —\n" +
        "• Target opponent loses 2 life and you gain 2 life.\n" +
        "• Choose target battle. If an opponent protects it, remove three defense counters from it. " +
        "Otherwise, put three defense counters on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Target opponent loses 2 life and you gain 2 life.") {
                val opponent = target(Targets.Opponent)
                effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
            },
            mode("Choose target battle. If an opponent protects it, remove three defense counters from it. Otherwise, put three defense counters on it.") {
                val battle = target(TargetFilter.Battle)
                effect = Effects.If(
                    condition = Conditions.TargetMatchesFilter(GameObjectFilter.Battle.protectedBy(), battle),
                    then = Effects.RemoveCounters(CounterType.DEFENSE, 3, battle),
                    otherwise = Effects.AddCounters(CounterType.DEFENSE, 3, battle)
                )
            }
        )
        description = "When this creature enters, choose one — Target opponent loses 2 life and you gain 2 life; " +
            "or choose target battle. If an opponent protects it, remove three defense counters from it. " +
            "Otherwise, put three defense counters on it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Helge C. Balzer"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2f1afa2-ca71-49cf-953b-d5dfc60c178f.jpg?1783917012"
    }
}
