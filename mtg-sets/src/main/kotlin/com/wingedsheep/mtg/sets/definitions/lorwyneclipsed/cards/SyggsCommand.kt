package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Sygg's Command
 * {1}{W}{U}
 * Kindred Sorcery — Merfolk
 *
 * Choose two —
 * • Create a token that's a copy of target Merfolk you control.
 * • Creatures target player controls gain lifelink until end of turn.
 * • Target player draws a card.
 * • Tap target creature. Put a stun counter on it.
 */
val SyggsCommand = card("Sygg's Command") {
    manaCost = "{1}{W}{U}"
    typeLine = "Kindred Sorcery — Merfolk"
    oracleText = "Choose two —\n" +
            "• Create a token that's a copy of target Merfolk you control.\n" +
            "• Creatures target player controls gain lifelink until end of turn.\n" +
            "• Target player draws a card.\n" +
            "• Tap target creature. Put a stun counter on it."

    spell {
        modal(chooseCount = 2) {
            mode("Create a token that's a copy of target Merfolk you control") {
                val merfolk = target(
                    "target Merfolk you control",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Creature.youControl().withSubtype("Merfolk")))
                )
                effect = Effects.CreateTokenCopyOfTarget(merfolk)
            }
            mode("Creatures target player controls gain lifelink until end of turn") {
                val player = target("target player", TargetPlayer())
                effect = EffectPatterns.grantKeywordToAll(
                    keyword = Keyword.LIFELINK,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                )
            }
            mode("Target player draws a card") {
                val player = target("target player", TargetPlayer())
                effect = Effects.DrawCards(1, player)
            }
            mode("Tap target creature. Put a stun counter on it") {
                val creature = target("target creature", TargetCreature())
                effect = Effects.Tap(creature)
                    .then(Effects.AddCounters(Counters.STUN, 1, creature))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Margaret Organ-Kean"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bc13fc2-e254-4db5-ad4d-f92711a1a6ca.jpg?1767862721"

        ruling("2025-11-17", "The token created by the first mode of Sygg's Command copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or is a token).")
        ruling("2025-11-17", "One or more stun counters on a permanent create a single replacement effect that stops the permanent from untapping. That effect is \"If a permanent with a stun counter on it would become untapped, instead remove a stun counter from it.\"")
        ruling("2025-11-17", "If all of Sygg's Command's targets are illegal as it tries to resolve, it will do nothing. If at least one target is still legal, it will resolve and do as much as it can.")
    }
}
