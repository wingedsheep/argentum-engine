package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Tangleroot — Mirrodin #259
 * {3} · Artifact
 *
 * Whenever a player casts a creature spell, that player adds {G}.
 *
 * A symmetric observer trigger — [Triggers.anyPlayerCasts]`(Creature)` fires for every player,
 * including Tangleroot's controller. The payoff is the cross-player mana shape:
 * [Effects.AddManaOfChoice] over a single-colour [ManaColorSet.Specific] (resolving to one colour
 * needs no choice) with `recipient = PlayerRef(TriggeringPlayer)`, so the {G} lands in the *caster's*
 * pool rather than the controller's.
 *
 * This is a triggered ability, not a mana ability (CR 605.1a — it triggers, and it doesn't resolve
 * as part of paying a cost), so it uses the stack and the mana arrives *after* the creature spell
 * has already been paid for. Per the 2011 ruling the caster gets the mana whether they want it or
 * not, and it drains with the pool at the end of the current step or phase.
 */
val Tangleroot = card("Tangleroot") {
    manaCost = "{3}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "Whenever a player casts a creature spell, that player adds {G}."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(GameObjectFilter.Creature)
        effect = Effects.AddManaOfChoice(
            colorSet = ManaColorSet.Specific(setOf(Color.GREEN)),
            recipient = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Dana Knutson"
        flavorText = "As if there's glitch in the system, the Tangle sometimes folds in on itself, " +
            "throwing off sparks of mana in a mystifying display."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87ff2c28-770b-49cb-be10-35429233e048.jpg?1783944500"
        ruling(
            "2011-01-25",
            "The player gets the mana whether they want it or not. If it isn't spent, it will disappear " +
                "at the end of the current step (or phase)."
        )
    }
}
