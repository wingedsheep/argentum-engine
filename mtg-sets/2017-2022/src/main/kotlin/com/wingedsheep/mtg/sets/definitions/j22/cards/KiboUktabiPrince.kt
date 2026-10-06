package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kibo, Uktabi Prince
 * {2}{G}
 * Legendary Creature — Monkey Noble
 * 2/2
 *
 * {T}: Each player creates a colorless artifact token named Banana with "{T}, Sacrifice this
 * token: Add {R} or {G}. You gain 2 life."
 * Whenever an artifact an opponent controls is put into a graveyard from the battlefield, put a
 * +1/+1 counter on each creature you control that's an Ape or a Monkey.
 * Whenever Kibo attacks, defending player sacrifices an artifact of their choice.
 *
 * Banana is a predefined token (`PredefinedTokens.Banana`); `ForEachPlayer` rebinds the controller
 * per player so each player gets their own. The artifact trigger uses the ANY-binding
 * `Triggers.a(...).dies()` (as on Party Dude), so an opponent's sacrificed Banana feeds it.
 */
val KiboUktabiPrince = card("Kibo, Uktabi Prince") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Monkey Noble"
    power = 2
    toughness = 2
    oracleText = "{T}: Each player creates a colorless artifact token named Banana with \"{T}, Sacrifice this " +
        "token: Add {R} or {G}. You gain 2 life.\"\n" +
        "Whenever an artifact an opponent controls is put into a graveyard from the battlefield, put a +1/+1 " +
        "counter on each creature you control that's an Ape or a Monkey.\n" +
        "Whenever Kibo attacks, defending player sacrifices an artifact of their choice."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachPlayer(Player.Each, Effects.CreatePredefinedToken("Banana"))
        description = "Each player creates a Banana token."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.opponentControls()).dies()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withAnySubtype("Ape", "Monkey").youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "Whenever an artifact an opponent controls is put into a graveyard from the battlefield, " +
            "put a +1/+1 counter on each creature you control that's an Ape or a Monkey."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Sacrifice(GameObjectFilter.Artifact, 1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "40"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b71345a-c3e8-4b35-beb7-6347e41d7626.jpg?1783919180"
    }
}
