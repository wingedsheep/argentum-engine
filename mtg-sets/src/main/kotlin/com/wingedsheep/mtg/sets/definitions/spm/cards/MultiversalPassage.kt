package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.OnEnterRunEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Multiversal Passage
 * Land
 *
 * As this land enters, choose a basic land type. Then you may pay 2 life. If you don't,
 * it enters tapped.
 * This land is the chosen type.
 *
 * The whole "as this land enters" clause is one [OnEnterRunEffect] — the generic "as ~ enters,
 * run [effect]" self-replacement — wrapping a three-step composite of existing atoms. Both ETB
 * clauses must live in the *same* replacement: `PlayLandHandler` runs the first `OnEnterRunEffect`
 * inline and returns, so a separate `EntersTapped(payLifeCost = 2)` replacement would never be
 * consulted. Folding them keeps the printed order (choose type, then pay-or-tap).
 *
 *  1. `ChooseOption(BASIC_LAND_TYPE)` records the chosen basic land type in the pipeline.
 *  2. [Effects.SetLandType] with `fromChosenValueKey` + [Duration.Permanent] on
 *     `EffectTarget.Self` installs a permanent Layer-4 (TYPE) effect replacing this land's
 *     subtypes with the chosen one (CR 305.7), so the land *is* that type ("This land is the
 *     chosen type.") and gains its intrinsic mana ability (e.g. Island → "{T}: Add {U}"). This is
 *     the self / at-entry / permanent counterpart of Dream Thrush's targeted, end-of-turn
 *     `ChooseOption(BASIC_LAND_TYPE)` → `SetLandType`.
 *  3. [OptionalCostEffect] gating [PayLifeEffect]`(2)` — "you may pay 2 life" — with an empty
 *     `ifPaid` (paying is its own reward; the land stays untapped) and `ifNotPaid` =
 *     `Effects.Tap(EffectTarget.Self)`, the same "if you don't, this land enters tapped" rider the
 *     SOI shadow-land cycle (Game Trail, Port Town, …) uses for its decline branch.
 */
val MultiversalPassage = card("Multiversal Passage") {
    typeLine = "Land"
    oracleText = "As this land enters, choose a basic land type. Then you may pay 2 life. " +
        "If you don't, it enters tapped.\n" +
        "This land is the chosen type."

    replacementEffect(
        OnEnterRunEffect(
            Effects.Composite(
                // Choose a basic land type; this land becomes it permanently.
                Effects.ChooseOption(
                    optionType = OptionType.BASIC_LAND_TYPE,
                    storeAs = "chosenLandType",
                ),
                Effects.SetLandType(
                    target = EffectTarget.Self,
                    duration = Duration.Permanent,
                    fromChosenValueKey = "chosenLandType",
                ),
                // Then you may pay 2 life. If you don't, it enters tapped.
                OptionalCostEffect(
                    cost = PayLifeEffect(2),
                    ifPaid = Effects.Composite(),
                    ifNotPaid = Effects.Tap(EffectTarget.Self),
                ),
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Pablo Mendoza"
        flavorText = "New worlds await..."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5fb426a-5618-4dd4-9c51-0cc847be8c1d.jpg?1783905299"
    }
}
