package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Agent of Treachery — Core Set 2020 #43
 * {5}{U}{U} · Creature — Human Rogue · Rare
 * 2/3
 *
 * When this creature enters, gain control of target permanent.
 * At the beginning of your end step, if you control three or more permanents you don't own,
 * draw three cards.
 *
 * The steal has no duration — it lasts after Agent of Treachery leaves. The end-step draw is an
 * intervening "if" counting the permanents you control whose owner is someone else: the filter is
 * "not owned by you" rather than "owned by an opponent", which is exactly the printed wording.
 */
val AgentOfTreachery = card("Agent of Treachery") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, gain control of target permanent.\n" +
        "At the beginning of your end step, if you control three or more permanents you don't own, " +
        "draw three cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.GainControl(permanent)
        description = "gain control of target permanent."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouControlAtLeast(
            3,
            GameObjectFilter.Permanent.withControllerPredicate(
                ControllerPredicate.Not(ControllerPredicate.OwnedByYou)
            )
        )
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Igor Kieryluk"
        flavorText = "\"The more secrets you have, the more easily you become my pawn.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc6686e6-4535-49be-b0b3-e76464656cd2.jpg?1783933017"
    }
}
