package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fall of the Impostor — Kaldheim #208
 * {1}{G}{W} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Put a +1/+1 counter on up to one target creature.
 * III — Exile a creature with the greatest power among creatures target opponent controls.
 *
 * Chapter III targets only the opponent: their creatures are gathered on resolution, narrowed to
 * the greatest power among them, and the Saga's controller picks one on the battlefield (a tie is
 * the controller's choice, per the ruling). The creature isn't targeted, so protection or hexproof
 * doesn't stop it. An opponent with no creatures loses nothing.
 */
val FallOfTheImpostor = card("Fall of the Impostor") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Put a +1/+1 counter on up to one target creature.\n" +
        "III — Exile a creature with the greatest power among creatures target opponent controls."

    sagaChapter(1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    sagaChapter(2) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    sagaChapter(3) {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.ControlledPermanents(Player.TargetOpponent, GameObjectFilter.Creature))
            val greatest = filter(creatures, CollectionFilter.GreatestPower)
            val toExile = chooseExactly(
                1,
                from = greatest,
                chooser = Chooser.Controller,
                prompt = "Choose a creature with the greatest power to exile",
                useTargetingUI = true
            )
            exile(toExile)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7de696a-49c2-421d-a86c-bcffd68870c6.jpg?1783928198"
        ruling(
            "2021-02-05",
            "If the target player controls more than one creature tied for the greatest power among creatures " +
                "that player controls, you choose which one of them is exiled."
        )
        ruling(
            "2021-02-05",
            "The greatest power among creatures target opponent controls is determined solely by evaluating the " +
                "creatures that player controls at that time. You won't consider creatures controlled by any other player."
        )
        ruling(
            "2021-02-05",
            "The chapter III ability doesn't target any creatures. For example, a creature with protection from " +
                "white could be exiled this way."
        )
    }
}
