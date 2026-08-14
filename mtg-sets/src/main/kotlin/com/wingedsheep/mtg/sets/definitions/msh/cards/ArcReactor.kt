package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Arc Reactor — Marvel Super Heroes #243 (rare)
 * {5} · Artifact
 *
 * Improvise (Your artifacts can help cast this spell. Each artifact you tap after you're done
 * activating mana abilities pays for {1}.)
 * This artifact enters tapped.
 * {T}: Add {C}{C}{C}.
 *
 * All three lines are existing primitives:
 *  - **Improvise** ([Keyword.IMPROVISE], CR 702.126) — the whole {5} is generic, so every untapped
 *    artifact the caster controls can pay {1} of it; with five artifacts out it costs nothing.
 *  - "This artifact enters tapped" is the [EntersTapped] replacement effect (the same one the
 *    tapped-land cycle uses); it is why the Reactor can't improvise the very spell that casts it.
 *  - The mana ability is [Effects.AddColorlessMana] for 3 behind [Costs.Tap].
 */
val ArcReactor = card("Arc Reactor") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "This artifact enters tapped.\n" +
        "{T}: Add {C}{C}{C}."

    keywords(Keyword.IMPROVISE)

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}{C}{C}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "Maxim Ruabtsev"
        flavorText = "A true marvel of engineering."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/339acb17-4b8e-4836-9cc5-8a0a946ebc73.jpg?1783902892"
    }
}
