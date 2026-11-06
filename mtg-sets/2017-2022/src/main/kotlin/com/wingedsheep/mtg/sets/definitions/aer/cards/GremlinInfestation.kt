package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gremlin Infestation
 * {3}{R}
 * Enchantment — Aura
 * Enchant artifact
 * At the beginning of your end step, this Aura deals 2 damage to enchanted artifact's controller.
 * When enchanted artifact is put into a graveyard, create a 2/2 red Gremlin creature token.
 *
 * The end-step trigger is the Aura controller's ("your end step"), so the damage recipient is
 * found at resolution: gather the enchanted artifact, then deal damage to that permanent's
 * current controller (`controllerOf()` reads the projected controller, so a stolen artifact
 * points at its new controller). The graveyard trigger is `Triggers.attached.dies()` — the
 * same put-into-a-graveyard-from-the-battlefield event for a noncreature permanent — and the
 * Aura's controller creates the token (2017-02-09 ruling).
 */
val GremlinInfestation = card("Gremlin Infestation") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "At the beginning of your end step, this Aura deals 2 damage to enchanted artifact's controller.\n" +
        "When enchanted artifact is put into a graveyard, create a 2/2 red Gremlin creature token."

    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val artifact = gather(CardSource.Entity(EffectTarget.EnchantedPermanent), name = "enchantedArtifact")
            run(Effects.DealDamage(2, artifact.controllerOf()))
        }
        description = "At the beginning of your end step, this Aura deals 2 damage to enchanted artifact's controller."
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Gremlin"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b51438f5-c4d4-434a-b7ea-dee0b60303e4.jpg?1783936754"
        ruling(
            "2017-02-09",
            "When the enchanted artifact is put into a graveyard, Gremlin Infestation's controller creates the Gremlin token."
        )
    }
}
