package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Temple of the Dragon Queen
 * Land
 *
 * As this land enters, you may reveal a Dragon card from your hand. This land enters tapped
 * unless you revealed a Dragon card this way or you control a Dragon.
 * As this land enters, choose a color.
 * {T}: Add one mana of the chosen color.
 *
 * Fortified Beachhead's reveal shape ([Effects.MayRevealCardFromHand] inside [OnEnterRun], gated
 * by "you control a Dragon" — controlling one already keeps the land untapped, so the optional
 * reveal is skipped). The land is not a Dragon, so it never satisfies its own condition; the
 * reveal filter reads the creature type on any card (Kindred cards count), never the card's name.
 *
 * The color choice rides in the same [OnEnterRun] program as [Effects.ChooseColorForTarget] on
 * Self rather than a separate `EntersWithChoice(COLOR)`: a played land runs its OnEnterRun and
 * returns, so a second as-enters replacement would never prompt. ChooseColorForTarget stores the
 * same `ChoiceSlot.COLOR` cast choice that [Effects.AddManaOfChosenColor] reads.
 */
val TempleOfTheDragonQueen = card("Temple of the Dragon Queen") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "As this land enters, you may reveal a Dragon card from your hand. This land enters " +
        "tapped unless you revealed a Dragon card this way or you control a Dragon.\n" +
        "As this land enters, choose a color.\n" +
        "{T}: Add one mana of the chosen color."

    replacementEffect(
        OnEnterRun(
            Effects.If(
                condition = Conditions.Not(
                    Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON))
                ),
                then = Effects.MayRevealCardFromHand(
                    filter = GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
                    otherwise = Effects.EnterTapped(),
                ),
            ) then Effects.ChooseColorForTarget(EffectTarget.Self)
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChosenColor()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c79b3c6-ccbe-48ae-a1f7-ddfd37ecdbf5.jpg?1783926433"
        ruling(
            "2021-07-23",
            "A Dragon card is a card with the creature type Dragon in its type line. Similarly, a creature " +
                "on the battlefield is a Dragon if it has the Dragon creature type. A card that has \"Dragon\" " +
                "in the name (such as Temple of the Dragon Queen) isn't a Dragon card unless it also has the " +
                "Dragon creature type."
        )
    }
}
