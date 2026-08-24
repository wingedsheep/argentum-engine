package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Elrond, Moon-Reader
 * {2}{U}
 * Legendary Creature — Elf Noble
 * 3/3
 *
 * Whenever you activate an ability of a creature, draw a card. This ability triggers only once
 * each turn.
 * {5}{U}{U}: Exile up to two other target nonland permanents you control. Return those cards to
 * the battlefield under their owner's control at the beginning of the next end step.
 *
 * The trigger is [Triggers.activatesAbilityOf] with `includeManaAbilities = true`: the Oracle text
 * puts no "that isn't a mana ability" clause on it, and a mana ability is still an activated
 * ability (CR 605.3), so a creature's "{T}: Add {G}" fires it — the card's own ruling says so
 * explicitly. Elrond's trigger is not itself a mana ability (CR 605.1b needs one that could add
 * mana; drawing a card can't), so it uses the stack normally. `oncePerTurn` carries the "only once
 * each turn" clause; a per-permanent restriction would be wrong here since the clause bounds the
 * *ability*, not the creatures it watches. The filter is unrestricted by controller — the oracle
 * says "a creature", not "a creature you control" — so an ability of a creature you don't control
 * but may activate still triggers it.
 *
 * The activated ability is the blink pattern from Hide on the Ceiling: one target requirement of
 * `count = 2, optional = true` for "up to two", then [ForEachTargetEffect] running
 * [Patterns.Exile.exileUntilEndStep] per chosen permanent so each gets its own delayed return
 * trigger. "Other" is [TargetFilter.OtherNonlandPermanent]'s `excludeSelf`; Elrond can't blink
 * itself, which also means the delayed return is unaffected by his leaving.
 */
val ElrondMoonReader = card("Elrond, Moon-Reader") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Elf Noble"
    oracleText = "Whenever you activate an ability of a creature, draw a card. This ability " +
        "triggers only once each turn.\n" +
        "{5}{U}{U}: Exile up to two other target nonland permanents you control. Return those " +
        "cards to the battlefield under their owner's control at the beginning of the next end step."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.activatesAbilityOf(GameObjectFilter.Creature, includeManaAbilities = true)
        oncePerTurn = true
        effect = DrawCardsEffect(1)
    }

    activatedAbility {
        cost = Costs.Mana("{5}{U}{U}")
        target = TargetPermanent(
            count = 2,
            optional = true,
            filter = TargetFilter.OtherNonlandPermanent.youControl()
        )
        effect = ForEachTargetEffect(
            listOf(Patterns.Exile.exileUntilEndStep(EffectTarget.ContextTarget(0)))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "36"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbcb310c-be73-46f8-8e65-8632454ccc6e.jpg?1784376948"
    }
}
