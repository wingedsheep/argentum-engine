package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Welding Jar — Mirrodin #274
 * {0} · Artifact
 *
 * Sacrifice this artifact: Regenerate target artifact.
 *
 * Regeneration on an *artifact*, not a creature: [RegenerateEffect] banks a destruction-replacement
 * shield on whatever permanent it names, and the engine's destroy chokepoint consults that shield
 * for any permanent type. The shield's "tap it, remove it from combat, heal its damage" (CR 701.15a)
 * simply has less to do on a noncreature artifact — it still gets tapped.
 *
 * Targets are chosen before costs are paid (CR 601.2c before 601.2h), so Welding Jar may legally
 * target *itself* — and then sacrifices itself to pay, leaving the shield on an object that is
 * already gone. Legal, and useless; players choose another artifact.
 */
val WeldingJar = card("Welding Jar") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sacrifice this artifact: Regenerate target artifact."

    activatedAbility {
        cost = Costs.SacrificeSelf
        val t = target("target artifact", TargetPermanent(filter = TargetFilter.Artifact))
        effect = RegenerateEffect(t)
        description = "Sacrifice this artifact: Regenerate target artifact."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "274"
        artist = "Mark Brill"
        flavorText = "The wires crawl over broken metal and heat themselves to melting, filling " +
            "cracks quickly and efficiently."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42b7b73b-4800-4fc7-9a5c-93e00ea88498.jpg?1783944496"
    }
}
