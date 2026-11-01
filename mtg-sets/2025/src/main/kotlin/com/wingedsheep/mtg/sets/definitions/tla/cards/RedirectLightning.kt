package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Redirect Lightning
 * {R}
 * Instant — Lesson
 *
 * As an additional cost to cast this spell, pay 5 life or pay {2}.
 * Change the target of target spell or ability with a single target.
 *
 * The binary additional-cost fork (pay 5 life or pay {2}) is modeled with [ModalEffect],
 * mirroring Bitter Triumph's "discard a card or pay 3 life" and Deadly Precision's
 * "pay {4} or sacrifice" patterns. The spell is NOT modal in MTG terms (no "Choose one —"
 * wording), so countsAsModalSpell = false. Both modes carry the same retarget effect.
 *
 * [Effects.ChangeTarget] + `TargetFilter.SpellOrAbilityOnStack.withSingleTarget()` is the
 * Willbender / Return the Favor retarget; "with a single target" is a targeting restriction.
 */
val RedirectLightning = card("Redirect Lightning") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant — Lesson"
    oracleText = "As an additional cost to cast this spell, pay 5 life or pay {2}.\n" +
        "Change the target of target spell or ability with a single target."

    spell {
        effect = ModalEffect.chooseOne(
            // Pay 5 life
            Mode(
                effect = Effects.ChangeTarget(),
                targetRequirements = listOf(TargetObject(filter = TargetFilter.SpellOrAbilityOnStack.withSingleTarget())),
                description = "Pay 5 life — change the target of target spell or ability " +
                    "with a single target",
                additionalCosts = listOf(Costs.additional.PayLife(amount = 5))
            ),
            // Pay {2}
            Mode(
                effect = Effects.ChangeTarget(),
                targetRequirements = listOf(TargetObject(filter = TargetFilter.SpellOrAbilityOnStack.withSingleTarget())),
                description = "Pay {2} — change the target of target spell or ability " +
                    "with a single target",
                additionalManaCost = "{2}"
            ),
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Toni Infante"
        flavorText = "\"A Waterbender lets their defense become their offense, turning their " +
            "opponent's energy against them. I learned a way to do this with lightning.\"\n—Iroh"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b5b14a7-1fdd-4efc-b197-cadfa7f7c860.jpg?1764121040"
    }
}
