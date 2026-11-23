package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Illusionist's Bracers
 * {2}
 * Artifact — Equipment
 * Whenever an ability of equipped creature is activated, if it isn't a mana ability, copy that
 * ability. You may choose new targets for the copy.
 * Equip {3}
 *
 * - **The trigger is `Triggers.attached.hasAbilityActivated()`** (Imprison's shape): bound to the
 *   equipped creature, any activating player (`Player.Each`, "is activated" in the passive), and
 *   the default "isn't a mana ability" gate. Its triggering entity is the activated ability still
 *   on the stack beneath this trigger.
 * - **The copy is [Effects.CopyTargetSpellOrAbility] over [EffectTarget.TriggeringEntity]**, as on
 *   Rings of Brighthearth — the shared executor offers new targets (CR 707.10c) and carries the
 *   mode and X along. The copy is controlled by the Bracers' controller, matching the ruling for a
 *   Bracers equipped to an opponent's creature.
 * - Sacrificing the Bracers or the creature as part of the cost means the activation event fires
 *   after the attachment is gone, so nothing is copied — the fifth ruling, for free.
 */
val IllusionistsBracers = card("Illusionist's Bracers") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever an ability of equipped creature is activated, if it isn't a mana ability, " +
        "copy that ability. You may choose new targets for the copy.\nEquip {3}"

    triggeredAbility {
        trigger = Triggers.attached.hasAbilityActivated()
        effect = Effects.CopyTargetSpellOrAbility(EffectTarget.TriggeringEntity)
        description = "Whenever an ability of equipped creature is activated, if it isn't a mana " +
            "ability, copy that ability. You may choose new targets for the copy."
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Svetlin Velinov"
        flavorText = "\"It's easy to believe you're a god when you're twice as powerful as everyone else.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc06cf32-5d66-4772-845d-ff7649396092.jpg?1783940091"
        ruling(
            "2013-04-15",
            "A mana ability is an ability that (1) isn't a loyalty ability, (2) doesn't target, and " +
                "(3) could add mana when it resolves."
        )
        ruling(
            "2013-01-24",
            "The copy will have the same targets as the ability it's copying unless you choose new " +
                "ones. You may change any number of the targets, including all of them or none of " +
                "them. If, for one of the targets, you can't choose a new legal target, then it " +
                "remains unchanged (even if the current target is illegal)."
        )
        ruling(
            "2013-01-24",
            "If the ability is modal (that is, it says \"Choose one —\" or the like), the copy will " +
                "have the same mode. You can't choose a different one."
        )
        ruling("2013-01-24", "An activated ability is written in the form \"Cost: Effect.\"")
        ruling(
            "2013-01-24",
            "If the cost of an activated ability requires Illusionist's Bracers or the equipped " +
                "creature to be sacrificed, the ability won't be copied. At the time the ability is " +
                "considered activated (after all costs are paid), Illusionist's Bracers is no longer " +
                "equipped to that creature."
        )
        ruling(
            "2013-01-24",
            "If Illusionist's Bracers somehow becomes equipped to a creature an opponent controls, " +
                "and an activated ability (that isn't a mana ability) of that creature is activated, " +
                "you will control the copy of that ability."
        )
        ruling("2013-01-24", "If the ability has {X} in its cost, the value of X is copied.")
    }
}
