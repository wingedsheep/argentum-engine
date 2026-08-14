package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * M.O.D.O.K. — Marvel Super Heroes #106
 * {3}{B}{B} · Legendary Artifact Creature — Villain · 2/2
 *
 * Flying, lifelink
 * Mental Organism — Pay 3 life: M.O.D.O.K. connives. Activate only during your turn.
 * Designed Only for Killing — Creatures your opponents control get -1/-1.
 *
 * "Mental Organism" and "Designed Only for Killing" are ability words (CR 207.2c) — flavor only.
 *
 * The activated ability has no mana component at all: its whole cost is [Costs.PayLife] 3, gated
 * to your own turn by [ActivationRestriction.OnlyDuringYourTurn] (there is no once-per-turn cap,
 * so it can be repeated as long as you can pay). [Effects.Connive] (CR 701.50) is the standard
 * draw-then-discard-then-counter-if-nonland pipeline, targeting M.O.D.O.K. itself.
 *
 * The shrink is a plain Layer 7c [ModifyStats] over [GroupFilter.AllCreaturesOpponentsControl] —
 * controller-relative, so it follows M.O.D.O.K. if it changes controllers, and it applies in the
 * projection rather than as counters (a creature it kills dies to state-based actions with 0
 * toughness).
 */
val Modok = card("M.O.D.O.K.") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact Creature — Villain"
    power = 2
    toughness = 2
    oracleText = "Flying, lifelink\n" +
        "Mental Organism — Pay 3 life: M.O.D.O.K. connives. Activate only during your turn. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter " +
        "on this creature.)\n" +
        "Designed Only for Killing — Creatures your opponents control get -1/-1."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.PayLife(3)
        effect = Effects.Connive()
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        description = "Pay 3 life: M.O.D.O.K. connives."
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = -1,
            filter = GroupFilter.AllCreaturesOpponentsControl
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "106"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38e87542-50f7-4812-9338-84e4b9b7bb44.jpg?1783902940"
    }
}
