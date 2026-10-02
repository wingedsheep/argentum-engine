package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Mycosynth Gardens — Phyrexia: All Will Be One #256
 * Land — Sphere
 *
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 * {X}, {T}: This land becomes a copy of target nontoken artifact you control with mana value X.
 *
 * The copy is the single-permanent shape of [Effects.EachPermanentBecomesCopyOfTarget]
 * (`affected = Self`) with the default permanent duration — no "until end of turn" in the text, so
 * the land keeps the artifact's copiable values for good (CR 707.2) and loses its own abilities.
 * The X paid threads into the target filter via `manaValueEqualsX()`, as on Hearth Kami.
 */
val TheMycosynthGardens = card("The Mycosynth Gardens") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Sphere"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Add one mana of any color.\n" +
        "{X}, {T}: This land becomes a copy of target nontoken artifact you control with mana value X."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        val artifact = target(
            TargetFilter(GameObjectFilter.Artifact.nontoken().youControl().manaValueEqualsX())
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = artifact,
            affected = EffectTarget.Self,
        )
        description = "{X}, {T}: This land becomes a copy of target nontoken artifact you control " +
            "with mana value X."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Andrew Mar"
        flavorText = "As the Invasion Tree grew, its branches pierced through the world's silent core."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a931463-25f6-4e31-95b4-bb4a9388009b.jpg?1783917979"
        ruling(
            "2023-02-04",
            "The Mycosynth Gardens copies exactly what was printed on the original artifact and nothing else " +
                "(unless that permanent is copying something else). It doesn't copy whether that artifact is " +
                "tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, and so on."
        )
        ruling(
            "2023-02-04",
            "If the copied artifact is copying something else, then The Mycosynth Gardens becomes a copy of " +
                "whatever that artifact copied."
        )
        ruling("2023-02-04", "If the copied artifact has {X} in its mana cost, X is 0.")
    }
}
