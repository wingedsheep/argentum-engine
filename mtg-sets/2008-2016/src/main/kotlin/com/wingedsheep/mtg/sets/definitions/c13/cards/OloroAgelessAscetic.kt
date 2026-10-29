package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oloro, Ageless Ascetic
 * {3}{W}{U}{B}
 * Legendary Creature — Giant Soldier
 * 4/5
 * At the beginning of your upkeep, you gain 2 life.
 * Whenever you gain life, you may pay {1}. If you do, draw a card and each opponent loses 1 life.
 * At the beginning of your upkeep, if Oloro is in the command zone, you gain 2 life.
 *
 * The first two abilities function only on the battlefield (the default). The third states the
 * zone it functions in, so per CR 113.6b it functions only from the command zone — the same
 * eminence-style shape as Edgar Markov, with `triggerZones = {COMMAND}` making the upkeep pass scan
 * the command zone for it, and the printed "if" kept as an intervening-if (CR 603.4) so an Oloro
 * cast out of the command zone in response gains nothing. You decide whether to pay {1} as the
 * second ability resolves.
 */
val OloroAgelessAscetic = card("Oloro, Ageless Ascetic") {
    manaCost = "{3}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Giant Soldier"
    power = 4
    toughness = 5
    oracleText = "At the beginning of your upkeep, you gain 2 life.\n" +
        "Whenever you gain life, you may pay {1}. If you do, draw a card and each opponent loses 1 life.\n" +
        "At the beginning of your upkeep, if Oloro is in the command zone, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.GainLife(2)
        description = "At the beginning of your upkeep, you gain 2 life."
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.DrawCards(1) then
                Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)),
        )
        description = "Whenever you gain life, you may pay {1}. If you do, draw a card and each " +
            "opponent loses 1 life."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerZones = setOf(Zone.COMMAND)
        interveningIf = Conditions.SourceInZone(Zone.COMMAND)
        effect = Effects.GainLife(2)
        description = "At the beginning of your upkeep, if Oloro is in the command zone, you gain 2 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "203"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abf8df47-405c-42d8-be9e-0f0d0a49589b.jpg?1783939648"
        ruling("2013-10-17", "Oloro's first ability works only if Oloro is on the battlefield.")
        ruling("2013-10-17", "If Oloro is your commander, its last ability will trigger at the beginning of the upkeep step on your first turn.")
        ruling("2013-10-17", "Oloro's second ability triggers just once for each life-gaining event, no matter how much life is gained.")
        ruling("2013-10-17", "You decide whether to pay {1} as the second ability resolves. If you do, you'll draw a card and each opponent will lose 1 life.")
    }
}
