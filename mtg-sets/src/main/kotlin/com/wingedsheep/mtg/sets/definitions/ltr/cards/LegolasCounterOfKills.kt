package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legolas, Counter of Kills
 * {2}{G}{U}
 * Legendary Creature — Elf Archer
 * 2/3
 *
 * Reach
 * Whenever you scry, if Legolas is tapped, you may untap it. Do this only
 * once each turn.
 * Whenever a creature an opponent controls dies, put a +1/+1 counter on
 * Legolas.
 */
val LegolasCounterOfKills = card("Legolas, Counter of Kills") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Archer"
    power = 2
    toughness = 3
    oracleText = "Reach\n" +
        "Whenever you scry, if Legolas is tapped, you may untap it. Do this only once each turn.\n" +
        "Whenever a creature an opponent controls dies, put a +1/+1 counter on Legolas."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.WheneverYouScry
        triggerCondition = Conditions.SourceIsTapped
        // "Do this only once each turn" is CR 603.2h — keyed to the untap, not to the trigger.
        // The official ruling is the clearest statement of the difference there is: "'Do this only
        // once each turn' lets you choose whether or not to untap Legolas as the triggered ability
        // resolves. If you don't untap it, the ability will trigger again the next time the
        // condition is met. Once you choose to do so, the ability will no longer trigger for the
        // rest of the turn." `oncePerTurn` would burn the turn's untap on a declined first scry.
        effectOncePerTurn = true
        effect = MayEffect(Effects.Untap(EffectTarget.Self))
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.opponentControls(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Yongjae Choi"
        flavorText = "\"Two?\" said Legolas. \"I make my tale twenty at the least.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8bd408c-9e6d-436d-9c4f-9ef3203aeb64.jpg?1686969862"
    }
}
