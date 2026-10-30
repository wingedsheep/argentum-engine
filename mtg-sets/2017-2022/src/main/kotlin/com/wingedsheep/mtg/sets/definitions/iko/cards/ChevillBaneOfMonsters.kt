package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chevill, Bane of Monsters
 * {B}{G}
 * Legendary Creature — Human Rogue
 * 1/3
 * Deathtouch
 * At the beginning of your upkeep, if your opponents control no permanents with bounty counters on
 * them, put a bounty counter on target creature or planeswalker an opponent controls.
 * Whenever a permanent an opponent controls with a bounty counter on it dies, you gain 3 life and
 * draw a card.
 *
 * The upkeep clause is an intervening "if" (checked as the trigger would fire and again on
 * resolution). Both abilities look at any bounty counter on an opponent's permanent, whichever
 * Chevill (or Termination Facilitator) put it there. The dies trigger matches the permanent's
 * last-known controller and counters, and "permanent" keeps it firing for a bountied object that
 * stopped being a creature or planeswalker.
 */
val ChevillBaneOfMonsters = card("Chevill, Bane of Monsters") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Rogue"
    power = 1
    toughness = 3
    oracleText = "Deathtouch\n" +
        "At the beginning of your upkeep, if your opponents control no permanents with bounty counters on them, put a bounty counter on target creature or planeswalker an opponent controls.\n" +
        "Whenever a permanent an opponent controls with a bounty counter on it dies, you gain 3 life and draw a card."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.OpponentControls(
            GameObjectFilter.Permanent.withCounter(CounterType.BOUNTY),
            negate = true
        )
        val mark = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker).opponentControls())
        effect = Effects.AddCounters(CounterType.BOUNTY, 1, mark)
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Permanent.opponentControls().withCounter(CounterType.BOUNTY)
        ).dies()
        effect = Effects.GainLife(3) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fecbf0a3-ebe1-43b6-a720-462ba19002eb.jpg?1783931028"
        ruling("2020-04-17", "Chevill's middle ability and last ability look for any bounty counter on your opponents' permanents, not just one from that Chevill. For example, if players A and B each control a Chevill and player A has put a bounty counter on player C's creature, player B's Chevill won't put a counter on anything and both players will gain 3 life and draw a card when that creature dies.")
        ruling("2020-04-17", "If the creature or planeswalker with a bounty counter on it ceases to be a creature or planeswalker, Chevill's last ability will still trigger if that permanent is put into a graveyard from the battlefield.")
        ruling("2020-04-17", "If Chevill leaves the battlefield at the same time that an opponent's permanent with a bounty counter on it dies, you gain 3 life and draw a card. If Chevill leaves the battlefield before that permanent dies, Chevill's last ability won't be around to trigger.")
    }
}
