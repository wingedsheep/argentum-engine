package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Blinkmoth Well — Mirrodin #279 (canonical printing, only printing)
 * Land
 *
 * {T}: Add {C}.
 * {2}, {T}: Tap target noncreature artifact.
 *
 * Two plain activated abilities. The second shares the `IsArtifact + IsNoncreature` target
 * filter shape used by Overwhelming Surge — it deliberately misses artifact *creatures*
 * (including one an opponent has animated with Karn or Titania's Song), which is exactly what
 * keeps this a soft answer to Mindslaver and the Blinkmoth Urn family rather than generic
 * creature removal. Both abilities cost the land's own tap, so they compete for it.
 */
private val NoncreatureArtifact = TargetFilter(
    GameObjectFilter(cardPredicates = listOf(CardPredicate.IsArtifact, CardPredicate.IsNoncreature))
)

val BlinkmothWell = card("Blinkmoth Well") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{2}, {T}: Tap target noncreature artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val artifact = target("noncreature artifact", TargetPermanent(filter = NoncreatureArtifact))
        effect = Effects.Tap(artifact)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "279"
        artist = "David Martin"
        flavorText = "When dictated by blinkmoth migratory patterns, clouds of tiny lights well up from Mirrodin's core."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4c79155-b6d8-46df-891f-487b24c4e0d5.jpg?1783944494"
    }
}
