package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Speed, Young Avenger — Marvel Super Heroes #152
 * {1}{R} · Legendary Creature — Mutant Hero · 2/2
 *
 * Haste
 * Whenever you cast a noncreature spell, you may pay {1}. When you do, target creature with haste
 * can't be blocked this turn except by creatures with haste.
 *
 * Implementation notes:
 *  - The pay-then-target shape is the Thousand Moons Crackshot / Grim Reaper idiom: a
 *    [ReflexiveTriggerEffect] whose action is the optional [PayManaCostEffect] and whose reflexive
 *    half picks its target only *after* the payment (CR 603.12). Declining the {1} therefore never
 *    locks a creature as a target, and the trigger has no target of its own — so it still goes on
 *    the stack when you control no hasty creature.
 *  - The evasion grant is exactly Resilient Roadrunner's
 *    [Effects.GrantCantBeBlockedExceptBy] with a `Creature.withKeyword(HASTE)` blocker filter; it
 *    lasts until end of turn ("this turn") and the blocker restriction is re-evaluated as blockers
 *    are declared, so a creature that gains or loses haste in the meantime is judged then.
 *  - The target itself must have haste, checked against projected state when targets are chosen
 *    and again on resolution (CR 608.2b): a creature that loses haste in response makes the
 *    reflexive ability fizzle.
 */
val SpeedYoungAvenger = card("Speed, Young Avenger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Mutant Hero"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "Whenever you cast a noncreature spell, you may pay {1}. When you do, target creature " +
        "with haste can't be blocked this turn except by creatures with haste."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = ReflexiveTriggerEffect(
            // "you may pay {1}"
            action = PayManaCostEffect(ManaCost.parse("{1}")),
            optional = true,
            // "When you do, target creature with haste can't be blocked this turn except by
            //  creatures with haste."
            reflexiveEffect = Effects.GrantCantBeBlockedExceptBy(
                EffectTarget.ContextTarget(0),
                GameObjectFilter.Creature.withKeyword(Keyword.HASTE),
            ),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.HASTE))
                )
            ),
            descriptionOverride = "You may pay {1}. When you do, target creature with haste can't " +
                "be blocked this turn except by creatures with haste.",
        )
        description = "Whenever you cast a noncreature spell, you may pay {1}. When you do, " +
            "target creature with haste can't be blocked this turn except by creatures with haste."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Tyler Walpole"
        flavorText = "\"Wait? Sorry, not in my vocabulary.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d15d187b-dc5d-482d-a79a-b9b98193ad7a.jpg?1783902923"
    }
}
