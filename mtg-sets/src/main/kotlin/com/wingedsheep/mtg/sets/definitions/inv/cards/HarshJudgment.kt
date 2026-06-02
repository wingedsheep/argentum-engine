package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harsh Judgment
 * {2}{W}{W}
 * Enchantment
 * As this enchantment enters, choose a color.
 * If an instant or sorcery spell of the chosen color would deal damage to you, it deals that
 * damage to its controller instead.
 *
 * Invasion engine gap #7: this is the first card to use the [RedirectDamage] static
 * replacement (previously defined but unwired). The chosen color is stored at ETB via
 * [EntersWithChoice] (ChoiceType.COLOR → ChosenColorComponent); the source filter combines
 * `InstantOrSorcery` with [CardPredicate.SharesChosenColorWithSource]; and the redirect
 * destination uses the new [EffectTarget.ControllerOfDamageSource].
 */
val HarshJudgment = card("Harsh Judgment") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a color.\n" +
        "If an instant or sorcery spell of the chosen color would deal damage to you, it deals that damage to its controller instead."

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    replacementEffect(
        RedirectDamage(
            redirectTo = EffectTarget.ControllerOfDamageSource,
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.You,
                source = SourceFilter.Matching(
                    GameObjectFilter.InstantOrSorcery.sharingChosenColorWithSource()
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34c78dee-ab45-4638-b89a-10686145b19a.jpg?1562905655"
    }
}
