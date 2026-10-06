package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jade Statue
 * {4}
 * Artifact
 * {2}: This artifact becomes a 3/6 Golem artifact creature until end of combat. Activate only during combat.
 *
 * Mishra's Factory's animate shape — [Effects.BecomeCreature] on itself, additive Artifact type plus
 * the Golem subtype — with [Duration.EndOfCombat] instead of end of turn, so `CombatManager.endCombat`
 * sweeps the floating effects when the combat phase ends. "Activate only during combat" is
 * [ActivationRestriction.DuringPhase] on [Phase.COMBAT], which covers every combat step on any
 * player's turn.
 */
val JadeStatue = card("Jade Statue") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}: This artifact becomes a 3/6 Golem artifact creature until end of combat. " +
        "Activate only during combat."

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 6,
            creatureTypes = setOf(Subtype.GOLEM.value),
            addTypes = setOf(CardType.ARTIFACT.name),
            duration = Duration.EndOfCombat,
        )
        restrictions = listOf(ActivationRestriction.DuringPhase(Phase.COMBAT))
        description = "{2}: This artifact becomes a 3/6 Golem artifact creature until end of combat. " +
            "Activate only during combat."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Dan Frazier"
        flavorText = "\"Some of the other guys dared me to touch it, but I knew it weren't no ordinary " +
            "hunk o' rock.\"\n—Norin the Wary"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d82d94b-ceef-4533-a4f2-b6442a61b839.jpg?1783948664"
        ruling(
            "2013-09-20",
            "If Jade Statue is animated by some other effect, you get an artifact creature with whatever " +
                "power, toughness, and creature types (if any) are specified by that effect. If you " +
                "subsequently use Jade Statue's ability during combat, it will become 3/6 and gain the " +
                "creature type Golem in addition to those creature types until end of combat."
        )
        ruling(
            "2005-08-01",
            "If Jade Statue's ability has been activated, abilities that trigger \"at end of combat\" " +
                "will see Jade Statue as an artifact creature."
        )
    }
}
