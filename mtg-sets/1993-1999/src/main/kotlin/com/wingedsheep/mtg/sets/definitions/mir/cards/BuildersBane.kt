package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Builder's Bane
 * {X}{X}{R}
 * Sorcery
 *
 * Destroy X target artifacts. Builder's Bane deals damage to each player equal to the
 * number of artifacts they controlled that were put into a graveyard this way.
 *
 * Pipeline:
 *   1. `GatherCards(ChosenTargets)` → references the resolved targets.
 *   2. `CaptureControllers` → snapshot each target's controller (stripped on destroy).
 *   3. `MoveCollection(..., MoveType.Destroy, storeMovedAs = "destroyed")` → destroys
 *      via the standard path; indestructible / regenerated / redirected targets drop
 *      out of `"destroyed"` (the "put into a graveyard this way" filter).
 *   4. `ForEachCapturedController` → for each player who lost ≥ 1 artifact this way,
 *      run the sub-effects with `controllerId` = that player and the per-iteration
 *      kill count in `storedNumbers["killCount"]`. The sub-effect is plain
 *      `DealDamage(VariableReference("killCount"), Controller)`.
 *
 * Target count is exactly X (`targets(…, exactly = X)`), surfaced as `xConstrainsTargetCount` +
 * `xConstrainsTargetCountExactly` on the LegalAction: the client requires exactly the X the player
 * chose at cast time, `TargetValidator` rejects a cast with any other number, and an X above the
 * number of artifacts isn't offered (CR 601.2c). X = 0 is a legal cast with no targets.
 */
val BuildersBane = card("Builder's Bane") {
    manaCost = "{X}{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy X target artifacts. Builder's Bane deals damage to each player " +
        "equal to the number of artifacts they controlled that were put into a graveyard this way."

    spell {
        targets(TargetFilter.Artifact, exactly = DynamicAmounts.xValue())
        effect = Effects.Pipeline {
            val targets = gather(CardSource.ChosenTargets)
            val preControllers = captureControllers(targets)
            val destroyed = moveTracked(targets, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy)
            forEachCaptured(destroyed, original = targets, controllers = preControllers) { killCount ->
                run(Effects.DealDamage(amount = killCount.amount, target = EffectTarget.Controller))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Charles Gillespie"
        flavorText = "There is only so much a person may be buried with."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb398027-5a29-4c81-aab5-b1a2b82fd655.jpg?1562722867"
    }
}
