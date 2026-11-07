package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Deadlock Trap — Kaladesh #204
 * {3} Artifact
 *
 * This artifact enters tapped.
 * When this artifact enters, you get {E}{E} (two energy counters).
 * {T}, Pay {E}: Tap target creature or planeswalker. Its activated abilities can't be activated
 * this turn.
 *
 * The lock is Koma, Cosmos Serpent's shape: the target is granted [PreventActivatedAbilities] over
 * itself until end of turn, so it holds even if the permanent untaps again, and it covers mana and
 * loyalty abilities too.
 */
val DeadlockTrap = card("Deadlock Trap") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "When this artifact enters, you get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}: Tap target creature or planeswalker. Its activated abilities can't be activated this turn."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        val permanent = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Tap(permanent) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = permanent,
                duration = Duration.EndOfTurn
            )
        description = "{T}, Pay {E}: Tap target creature or planeswalker. Its activated abilities can't be " +
            "activated this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f956dedc-2cf4-4039-8302-feab94b78426.jpg?1783937160"
        ruling(
            "2016-09-20",
            "Players may respond to Deadlock Trap's last ability by activating an ability of the target " +
                "permanent if that ability's timing permissions allow it."
        )
        ruling(
            "2016-09-20",
            "If the tapped creature somehow becomes untapped, it can attack and block as normal, but its " +
                "abilities still can't be activated this turn."
        )
        ruling("2016-09-20", "A tapped planeswalker can be attacked or dealt damage as normal.")
    }
}
