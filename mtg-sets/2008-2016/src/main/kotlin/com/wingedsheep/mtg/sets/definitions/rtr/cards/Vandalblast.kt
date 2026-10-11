package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Vandalblast — Return to Ravnica #111 (canonical printing)
 * {R} · Sorcery
 *
 * Destroy target artifact you don't control.
 * Overload {4}{R}
 *
 * "You don't control" is `Not(ControlledByYou)`, mirroring Cyclonic Rift. Overloaded, the spell has
 * no targets (CR 702.96b), so it also destroys hexproof and protected artifacts.
 */
val Vandalblast = card("Vandalblast") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact you don't control.\n" +
        "Overload {4}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{4}{R}"))

    spell {
        val notYours = GameObjectFilter.Artifact.withControllerPredicate(
            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
        )
        val artifact = target(TargetFilter(notYours))
        effect = Effects.Destroy(artifact)

        overloadEffect = Effects.DestroyAll(notYours)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Seb McKinnon"
        flavorText = "Beauty is in the eye of the exploder."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5925c559-3e3c-481b-ba95-20a405cbffce.jpg?1783940352"
    }
}
