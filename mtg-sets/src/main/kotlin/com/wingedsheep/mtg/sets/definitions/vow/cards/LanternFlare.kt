package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lantern Flare
 * {1}{W}
 * Instant
 * Cleave {X}{R}{W} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Lantern Flare deals X damage to target creature or planeswalker and you gain X life. [X is the
 * number of creatures you control.]
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. Here the
 * *cleave cost itself carries {X}*, and the bracket defines X for the printed mode — so the two
 * modes read X from different sources:
 *  - Printed cast (brackets present): X is fixed by the board — [DynamicAmounts.creaturesYouControl].
 *  - Cleaved cast (brackets removed): X is the value chosen and paid for the {X} in the cleave
 *    cost, read at resolution via [DynamicAmount.XValue]. The cleave enumerator computes the
 *    affordable-X ceiling and threads the chosen X through payment → resolution (the engine's
 *    X-on-cleave support), so `DynamicAmount.XValue` sees it here.
 *
 * The target ("creature or planeswalker") is identical in both modes, so only the effect differs
 * (via [SpellBuilder.cleaveEffect]) — the shared shape is X damage to the target plus gain X life,
 * with only the amount source swapped. The life gain is unconditional (not tied to damage dealt).
 */
val LanternFlare = card("Lantern Flare") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Cleave {X}{R}{W} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\n" +
        "Lantern Flare deals X damage to target creature or planeswalker and you gain X life. " +
        "[X is the number of creatures you control.]"

    keywordAbility(KeywordAbility.cleave("{X}{R}{W}"))

    spell {
        // Printed (brackets present): X = the number of creatures you control.
        val printedX = DynamicAmounts.creaturesYouControl()
        val target = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(printedX, target)
            .then(Effects.GainLife(printedX))

        // Cleaved (brackets removed): X is the amount paid for the {X} in the cleave cost.
        val cleaveTarget = cleaveTarget("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        cleaveEffect = Effects.DealDamage(DynamicAmount.XValue, cleaveTarget)
            .then(Effects.GainLife(DynamicAmount.XValue))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd59300-7b5c-4263-a7fd-775c9fcb05ad.jpg?1783924916"
    }
}
