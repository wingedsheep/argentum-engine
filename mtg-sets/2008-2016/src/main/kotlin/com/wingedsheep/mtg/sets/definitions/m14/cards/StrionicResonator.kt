package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Strionic Resonator
 * {2}
 * Artifact
 * {2}, {T}: Copy target triggered ability you control. You may choose new targets for the copy.
 * (A triggered ability uses the words "when," "whenever," or "at.")
 */
val StrionicResonator = card("Strionic Resonator") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Copy target triggered ability you control. You may choose new targets for the copy. " +
        "(A triggered ability uses the words \"when,\" \"whenever,\" or \"at.\")"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val ability = target(TargetFilter.TriggeredAbilityOnStack.youControl())
        effect = Effects.CopyTargetTriggeredAbility(ability)
        // Hold priority while a triggered ability we control is on the stack, so it can be
        // copied before it resolves (gated on the target type, like Kirol, Attentive First-Year).
        holdPriority = true
        description = "{2}, {T}: Copy target triggered ability you control. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Noah Bradley"
        flavorText = "Its tines cannot be rung by mere physical force. Only magic of great power can start its eldritch resonance."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94d1fc0f-5c8b-4e47-aaf8-8888c025f70f.jpg?1783939893"
    }
}
