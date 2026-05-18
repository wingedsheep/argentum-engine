package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CaptureControllersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachCapturedControllerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount


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
 * Target count is X. `TargetObject.dynamicMaxCount = XValue` surfaces as
 * `xConstrainsTargetCount = true` on the LegalAction; the client clamps the targeting
 * overlay's max selection to the X the player chose at cast time, and `TargetValidator`
 * rejects casts whose `targets.size` exceeds that X. `optional = true` so X=0 (legal
 * but degenerate) resolves cleanly and casts with fewer legal artifacts than X don't
 * fizzle outright.
 */
val BuildersBane = card("Builder's Bane") {
    manaCost = "{X}{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy X target artifacts. Builder's Bane deals damage to each player " +
        "equal to the number of artifacts they controlled that were put into a graveyard this way."

    spell {
        target = TargetPermanent(
            optional = true,
            filter = TargetFilter.Artifact,
            dynamicMaxCount = DynamicAmount.XValue
        )
        effect = Effects.Composite(
            GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "targets"),
            CaptureControllersEffect(from = "targets", storeAs = "preControllers"),
            MoveCollectionEffect(
                from = "targets",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy,
                storeMovedAs = "destroyed"
            ),
            ForEachCapturedControllerEffect(
                collection = "destroyed",
                originalCollection = "targets",
                controllerSnapshot = "preControllers",
                countVariable = "killCount",
                effects = listOf(
                    DealDamageEffect(
                        amount = DynamicAmount.VariableReference("killCount"),
                        target = EffectTarget.Controller
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Charles Gillespie"
        flavorText = "There is only so much a person may be buried with."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb398027-5a29-4c81-aab5-b1a2b82fd655.jpg?1562722867"
    }
}
