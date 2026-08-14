package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Totentanz, Swarm Piper
 * {1}{B}{R}
 * Legendary Creature — Human Warlock Bard
 * 2/3
 *
 * Whenever Totentanz or another nontoken creature you control dies, create a 1/1 black Rat
 * creature token with "This token can't block."
 * {1}{B}: Target attacking Rat you control gains deathtouch until end of turn.
 *
 * "Totentanz or another nontoken creature you control" is one per-creature death trigger over
 * nontoken creatures you control, bound [TriggerBinding.ANY] so Totentanz's own death counts —
 * the same shape [VengefulBloodwitch] uses for "this creature or another creature you control
 * dies". Per-creature (not once-per-event) is what the WOE ruling calls for: when Totentanz dies
 * alongside other nontoken creatures you control, the ability triggers for each of them, so a
 * board wipe makes a Rat for every one.
 *
 * The Rats it makes are tokens, so they never feed the trigger themselves — sacrificing the swarm
 * doesn't snowball. The Rat itself is WOE's shared type-named token via [woeRatToken].
 *
 * The deathtouch grant targets an *attacking* Rat, so it's only castable once blockers matter;
 * [TargetFilter] carries the attacking + Rat + you-control restriction so the engine rejects an
 * illegal choice at announcement rather than fizzling on resolution.
 */
val TotentanzSwarmPiper = card("Totentanz, Swarm Piper") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Warlock Bard"
    power = 2
    toughness = 3
    oracleText = "Whenever Totentanz or another nontoken creature you control dies, create a 1/1 " +
        "black Rat creature token with \"This token can't block.\"\n" +
        "{1}{B}: Target attacking Rat you control gains deathtouch until end of turn."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().nontoken(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = woeRatToken()
        description = "Whenever Totentanz or another nontoken creature you control dies, create a " +
            "1/1 black Rat creature token with \"This token can't block.\""
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        val rat = target(
            "target attacking Rat you control",
            TargetCreature(
                filter = TargetFilter.Creature.withSubtype(Subtype.RAT).youControl().attacking()
            ),
        )
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, rat)
        description = "{1}{B}: Target attacking Rat you control gains deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Matt Stewart"
        flavorText = "Among the rats, he found the acceptance the human world never gave him."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1422d6db-fe5b-4a89-951a-fbd7985a29fc.jpg?1783915068"

        ruling(
            "2023-09-01",
            "If Totentanz, Swarm Piper dies at the same time as one or more other nontoken creatures " +
                "you control, Totentanz's ability triggers for each of them."
        )
    }
}
