package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gogo, Master of Mimicry
 * {2}{U}
 * Legendary Creature — Wizard
 * 2/4
 *
 * {X}{X}, {T}: Copy target activated or triggered ability you control X times. You may choose new
 * targets for the copies. This ability can't be copied and X can't be 0. (Mana abilities can't be
 * targeted.)
 *
 * The copy loop reuses [Effects.CopyTargetSpellOrAbility] with `copies = DynamicAmount.XValue` — the
 * executor makes X independent copies of the chosen ability on the stack (CR 707.10), pausing per
 * copy that has targets so the controller may retarget each one (CR 707.10c). A no-target ability is
 * copied X times all the same (per the Gogo rulings — it can copy any ability you control on the
 * stack, not only targeted ones). The copies are pushed above the source, so they resolve first.
 *
 * "X can't be 0" is `minimumXValue = 1` (the X-choice decision clamps its lower bound and the
 * handler rejects a pre-filled X below it). "This ability can't be copied" is `cantBeCopied = true`,
 * which tags the ability instance on the stack so another copy-ability effect (a second Gogo) makes
 * no copy of it (CR 707.10e). `holdPriority` lets the player activate Gogo in response to one of
 * their own abilities on the stack, before it resolves. The `{X}{X}` cost pays X twice, so X is the
 * number of copies — read at resolution via [DynamicAmount.XValue].
 */
val GogoMasterOfMimicry = card("Gogo, Master of Mimicry") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Wizard"
    power = 2
    toughness = 4
    oracleText = "{X}{X}, {T}: Copy target activated or triggered ability you control X times. " +
        "You may choose new targets for the copies. This ability can't be copied and X can't be 0. " +
        "(Mana abilities can't be targeted.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}"), Costs.Tap)
        val ability = target(
            "target activated or triggered ability you control",
            Targets.ActivatedOrTriggeredAbilityYouControl
        )
        effect = Effects.CopyTargetSpellOrAbility(ability, copies = DynamicAmount.XValue)
        minimumXValue = 1
        cantBeCopied = true
        // Hold priority while an activated or triggered ability we control is on top of the stack —
        // otherwise it would resolve before we could copy it. The enumerator gates the hint on the
        // target type, so it only fires when a copyable ability is actually on top.
        holdPriority = true
        description = "{X}{X}, {T}: Copy target activated or triggered ability you control X times. " +
            "You may choose new targets for the copies. This ability can't be copied and X can't be 0."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "54"
        artist = "Thea Dumitriu"
        flavorText = "\"I see... so, you seek to save the world. Then I guess that means I shall " +
            "save the world as well. Lead on! I will copy your every move.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cce4eb99-d960-4ab7-911a-bb4ea74d1775.jpg?1782686556"
    }
}
