package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kurkesh, Onakke Ancient
 * {2}{R}{R}
 * Legendary Creature — Ogre Spirit
 * 4/3
 * Whenever you activate an ability of an artifact, if it isn't a mana ability, you may pay {R}.
 * If you do, copy that ability. You may choose new targets for the copy.
 *
 * Rings of Brighthearth's trigger with Harsh Mentor's source filter:
 *
 * - **"an ability of an artifact"** is `GameObjectFilter.Artifact.onBattlefield()` — an artifact
 *   permanent (the description names a card type without "card", so it means a permanent), read
 *   off projected state, so an artifact creature or an artifact planeswalker counts. The artifact
 *   needn't be yours; only the activation must be. A source the ability's own cost sacrifices (a
 *   Clue) is matched as it last existed on the battlefield.
 * - **"if it isn't a mana ability"** is the default gate of `activatesAbility()`, which excludes
 *   mana abilities and nothing else — loyalty abilities still trigger it.
 * - **"you may pay {R}. If you do, copy that ability"** is [Effects.MayPay] over
 *   [Effects.CopyTargetSpellOrAbility] on [EffectTarget.TriggeringEntity], the activated ability
 *   still on the stack beneath this trigger. The copy executor offers new targets (CR 707.10c)
 *   and keeps the original's mode and X, per the rulings.
 */
val KurkeshOnakkeAncient = card("Kurkesh, Onakke Ancient") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Ogre Spirit"
    power = 4
    toughness = 3
    oracleText = "Whenever you activate an ability of an artifact, if it isn't a mana ability, you may " +
        "pay {R}. If you do, copy that ability. You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.activatesAbility(of = GameObjectFilter.Artifact.onBattlefield())
        effect = Effects.MayPay(
            cost = Effects.PayMana("{R}"),
            then = Effects.CopyTargetSpellOrAbility(EffectTarget.TriggeringEntity),
            descriptionOverride = "Pay {R}? If you do, copy that ability. You may choose new " +
                "targets for the copy."
        )
        description = "Whenever you activate an ability of an artifact, if it isn't a mana ability, " +
            "you may pay {R}. If you do, copy that ability. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Slawomir Maniak"
        flavorText = "The Onakke were masters of dark arts and artifice. Their creations continue to " +
            "spread foul magic, paving the way for their return."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e2a0e9e-0e45-4af1-9f03-6fd3652933f9.jpg?1783939173"
        ruling(
            "2014-07-18",
            "A mana ability is an ability that (1) could add mana when it resolves, (2) isn't a " +
                "loyalty ability, and (3) doesn't require a target."
        )
        ruling(
            "2014-07-18",
            "The copy will have the same targets as the ability it's copying unless you choose new " +
                "ones. You may change any number of the targets, including all of them or none of " +
                "them. If, for one of the targets, you can't choose a new legal target, then it " +
                "remains unchanged (even if the current target is illegal)."
        )
        ruling(
            "2014-07-18",
            "If the ability is modal (that is, it says \"Choose one —\" or the like), the copy will " +
                "have the same mode. You can't choose a different one."
        )
        ruling("2014-07-18", "If the ability has {X} in its cost, the value of X is copied.")
        ruling(
            "2014-07-18",
            "If paying the activation cost of the ability includes sacrificing Kurkesh, the ability " +
                "won't be copied. At the time the ability is considered activated (after all costs " +
                "are paid), Kurkesh is no longer on the battlefield."
        )
    }
}
