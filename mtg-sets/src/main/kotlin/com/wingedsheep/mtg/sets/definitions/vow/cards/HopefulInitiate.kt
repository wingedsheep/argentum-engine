package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity

/**
 * Hopeful Initiate
 * {W}
 * Creature — Human Warlock
 * 1/2
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 * {2}{W}, Remove two +1/+1 counters from among creatures you control: Destroy target artifact
 * or enchantment.
 *
 * Canonical printing: VOW is Hopeful Initiate's earliest real-expansion printing, so the full
 * CardDefinition lives here; INR (2025) gets a `Printing` row only ([HopefulInitiateReprint]).
 *
 * Two independent pieces:
 *  - [training] gives the keyword + the attack trigger, which is also this card's own counter
 *    engine — the +1/+1 counters it accrues feed the activated ability's removal cost.
 *  - An activated ability whose cost is a [Costs.Composite] of {2}{W} and removing two +1/+1
 *    counters from among creatures you control ([Costs.RemoveCounters], already scoped to the
 *    activator's permanents). Paying it destroys a target artifact or enchantment. The counters
 *    may come from any creatures you control, not just the Initiate — so a wide +1/+1-counter
 *    board (Training, Cloaked Cadet, etc.) can fuel repeated activations.
 */
val HopefulInitiate = card("Hopeful Initiate") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warlock"
    power = 1
    toughness = 2
    oracleText = "Training (Whenever this creature attacks with another creature with greater " +
        "power, put a +1/+1 counter on this creature.)\n" +
        "{2}{W}, Remove two +1/+1 counters from among creatures you control: Destroy target " +
        "artifact or enchantment."

    training()

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{W}"),
            Costs.RemoveCounters(2, Counters.PLUS_ONE_PLUS_ONE, Filters.Creature),
        )
        val victim = target("target artifact or enchantment", Targets.ArtifactOrEnchantment)
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec1b3fd1-952a-4bc6-9b31-bd9bd13072f5.jpg?1783924918"
    }
}
